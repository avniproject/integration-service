package org.avni_integration_service.tanuh.worker;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.repository.AvniEncounterRepository;
import org.avni_integration_service.avni.repository.AvniSubjectRepository;
import org.avni_integration_service.integration_data.domain.AvniEntityType;
import org.avni_integration_service.integration_data.repository.ErrorRecordRepository;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.avni_integration_service.tanuh.config.TanuhContextProvider;
import org.avni_integration_service.tanuh.domain.ModelResult;
import org.avni_integration_service.tanuh.domain.OralScreeningInputs;
import org.avni_integration_service.tanuh.domain.ReviewCategory;
import org.avni_integration_service.tanuh.domain.TanuhConcepts;
import org.avni_integration_service.tanuh.model.HighRiskModelClient;
import org.avni_integration_service.tanuh.model.HighRiskModelException;
import org.avni_integration_service.tanuh.service.RoutingPolicy;
import org.avni_integration_service.tanuh.service.Sampler;
import org.avni_integration_service.tanuh.service.TanuhErrorService;
import org.avni_integration_service.tanuh.service.TanuhPhotoDownloader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class OralScreeningWorker {
    static final int PAGE_SIZE = 1000;
    private static final Logger logger = Logger.getLogger(OralScreeningWorker.class);
    // The run time is a true instant, so it is written in UTC.
    private static final DateTimeFormatter RUN_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private final AvniEncounterRepository avniEncounterRepository;
    private final AvniSubjectRepository avniSubjectRepository;
    private final TanuhPhotoDownloader tanuhPhotoDownloader;
    private final HighRiskModelClient highRiskModelClient;
    private final RoutingPolicy routingPolicy;
    private final Sampler sampler;
    private final TanuhErrorService tanuhErrorService;
    private final ErrorRecordRepository errorRecordRepository;
    private final IntegratingEntityStatusRepository integratingEntityStatusRepository;
    private final TanuhContextProvider tanuhContextProvider;
    private final int pageSize;

    @Autowired
    public OralScreeningWorker(AvniEncounterRepository avniEncounterRepository, AvniSubjectRepository avniSubjectRepository,
                               TanuhPhotoDownloader tanuhPhotoDownloader, HighRiskModelClient highRiskModelClient,
                               RoutingPolicy routingPolicy, Sampler sampler, TanuhErrorService tanuhErrorService,
                               ErrorRecordRepository errorRecordRepository,
                               IntegratingEntityStatusRepository integratingEntityStatusRepository,
                               TanuhContextProvider tanuhContextProvider) {
        this(avniEncounterRepository, avniSubjectRepository, tanuhPhotoDownloader, highRiskModelClient, routingPolicy, sampler,
                tanuhErrorService, errorRecordRepository, integratingEntityStatusRepository, tanuhContextProvider, PAGE_SIZE);
    }

    OralScreeningWorker(AvniEncounterRepository avniEncounterRepository, AvniSubjectRepository avniSubjectRepository,
                        TanuhPhotoDownloader tanuhPhotoDownloader, HighRiskModelClient highRiskModelClient,
                        RoutingPolicy routingPolicy, Sampler sampler, TanuhErrorService tanuhErrorService,
                        ErrorRecordRepository errorRecordRepository,
                        IntegratingEntityStatusRepository integratingEntityStatusRepository,
                        TanuhContextProvider tanuhContextProvider, int pageSize) {
        this.avniEncounterRepository = avniEncounterRepository;
        this.avniSubjectRepository = avniSubjectRepository;
        this.tanuhPhotoDownloader = tanuhPhotoDownloader;
        this.highRiskModelClient = highRiskModelClient;
        this.routingPolicy = routingPolicy;
        this.sampler = sampler;
        this.tanuhErrorService = tanuhErrorService;
        this.errorRecordRepository = errorRecordRepository;
        this.integratingEntityStatusRepository = integratingEntityStatusRepository;
        this.tanuhContextProvider = tanuhContextProvider;
        this.pageSize = pageSize;
    }

    // checkOpenErrorRecord is false only for the retry job (#132), which owns the screenings that have one.
    public ScreeningOutcome processScreening(GeneralEncounter s, boolean checkOpenErrorRecord) {
        String uuid = s.getUuid();
        try {
            if (!TanuhConcepts.ORAL_SCREENING.equals(s.getEncounterType()) || !s.isCompleted() || Boolean.TRUE.equals(s.getVoided()))
                return skipped(uuid, "not a completed, live Oral Screening");
            if (tanuhContextProvider.get().getAvniImplUser().equalsIgnoreCase(s.getLastModifiedBy()))
                return skipped(uuid, "last changed by the job");
            if (checkOpenErrorRecord && errorRecordRepository.findByAvniEntityTypeAndEntityId(AvniEntityType.GeneralEncounter, uuid) != null)
                return skipped(uuid, "waiting for a retry");
            if (Boolean.TRUE.equals(avniSubjectRepository.getSubjectOrThrow(s.getSubjectId()).getVoided()))
                return skipped(uuid, "patient deleted");
            OralScreeningInputs inputs = OralScreeningInputs.from(s);
            if (!inputs.hasPhotos()) {
                avniEncounterRepository.patch(uuid, notScored());
                logger.info(String.format("Screening %s has no photo: marked not scored", uuid));
                return ScreeningOutcome.WRITTEN;
            }
            ModelResult result = score(s, inputs);
            ReviewCategory category = routingPolicy.route(result.result(), inputs.getWorkerOpinion(), () -> sampler.draw(uuid));
            avniEncounterRepository.patch(uuid, scored(result, category));
            logger.info(String.format("Screening %s scored %s, group %s", uuid, result.result().getAnswer(), category.getAnswer()));
            return ScreeningOutcome.WRITTEN;
        } catch (HighRiskModelException e) {
            return failed(uuid, TanuhErrorService.MODEL_CALL_FAILED, e);
        } catch (Exception e) {
            return failed(uuid, TanuhErrorService.SCREENING_PROCESSING_FAILED, e);
        }
    }

    private ModelResult score(GeneralEncounter s, OralScreeningInputs inputs) throws HighRiskModelException {
        List<File> photos = new ArrayList<>();
        try {
            for (String url : inputs.getPhotoUrls()) photos.add(tanuhPhotoDownloader.download(url));
            return highRiskModelClient.score(s, photos);
        } finally {
            photos.forEach(File::delete);
        }
    }

    private static Map<String, Object> notScored() {
        Map<String, Object> values = new HashMap<>();
        values.put(TanuhConcepts.MODEL_STATUS, TanuhConcepts.STATUS_NOT_SCORED);
        values.put(TanuhConcepts.REVIEW_CATEGORY, ReviewCategory.NOT_SCORED.getAnswer());
        values.put(TanuhConcepts.MODEL_RESULT, null);
        values.put(TanuhConcepts.MODEL_VERSION, null);
        values.put(TanuhConcepts.MODEL_RUN_TIME, null);
        return values;
    }

    private static Map<String, Object> scored(ModelResult result, ReviewCategory category) {
        Map<String, Object> values = new HashMap<>();
        values.put(TanuhConcepts.MODEL_RESULT, result.result().getAnswer());
        values.put(TanuhConcepts.MODEL_STATUS, TanuhConcepts.STATUS_SCORED);
        values.put(TanuhConcepts.MODEL_VERSION, result.version());
        values.put(TanuhConcepts.MODEL_RUN_TIME, RUN_TIME.format(result.runTime()));
        values.put(TanuhConcepts.REVIEW_CATEGORY, category.getAnswer());
        return values;
    }

    private static ScreeningOutcome skipped(String uuid, String reason) {
        logger.debug(String.format("Screening %s skipped: %s", uuid, reason));
        return ScreeningOutcome.SKIPPED;
    }

    private ScreeningOutcome failed(String uuid, String errorTypeName, Exception e) {
        logger.error(String.format("Screening %s waits for a retry: %s", uuid, errorTypeName), e);
        tanuhErrorService.errorOccurred(uuid, errorTypeName, String.format("%s: %s", e.getClass().getSimpleName(), e.getMessage()));
        return ScreeningOutcome.FAILED;
    }
}
