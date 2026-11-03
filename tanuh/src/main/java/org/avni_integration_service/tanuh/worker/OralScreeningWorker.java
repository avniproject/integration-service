package org.avni_integration_service.tanuh.worker;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.avni.repository.AvniEncounterRepository;
import org.avni_integration_service.avni.repository.AvniSubjectRepository;
import org.avni_integration_service.integration_data.domain.AvniEntityType;
import org.avni_integration_service.integration_data.repository.ErrorRecordRepository;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.avni_integration_service.tanuh.config.TanuhConfig;
import org.avni_integration_service.tanuh.config.TanuhContextProvider;
import org.avni_integration_service.tanuh.domain.OralScreeningInputs;
import org.avni_integration_service.tanuh.domain.TanuhConcepts;
import org.avni_integration_service.tanuh.domain.TanuhConcepts.Concept;
import org.avni_integration_service.tanuh.dqs.DqsClient;
import org.avni_integration_service.tanuh.dqs.DqsException;
import org.avni_integration_service.tanuh.dqs.DqsResult;
import org.avni_integration_service.tanuh.dqs.OralScreeningRecord;
import org.avni_integration_service.tanuh.dqs.RegistrationRecord;
import org.avni_integration_service.tanuh.model.HighRiskModelClient;
import org.avni_integration_service.tanuh.model.HighRiskModelException;
import org.avni_integration_service.tanuh.service.TanuhErrorService;
import org.avni_integration_service.tanuh.service.TanuhPhotoDownloader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Each new or edited Oral Screening goes to the data quality service, and the QC verdict is written back to it. A
// screening that passed QC is then handed to the high-risk model service, which works on its own time and writes its
// result to Avni itself.
@Component
public class OralScreeningWorker {
    public static final String CURSOR_ENTITY_TYPE = "TanuhOralScreening";
    static final int PAGE_SIZE = EncounterCursorWalker.PAGE_SIZE;
    private static final Logger logger = Logger.getLogger(OralScreeningWorker.class);
    // The check time is a true instant, so it is written in UTC.
    private static final DateTimeFormatter CHECK_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private final AvniEncounterRepository avniEncounterRepository;
    private final AvniSubjectRepository avniSubjectRepository;
    private final DqsClient dqsClient;
    private final TanuhPhotoDownloader tanuhPhotoDownloader;
    private final HighRiskModelClient highRiskModelClient;
    private final TanuhErrorService tanuhErrorService;
    private final ErrorRecordRepository errorRecordRepository;
    private final TanuhContextProvider tanuhContextProvider;
    private final Clock clock;
    private final EncounterCursorWalker walker;

    @Autowired
    public OralScreeningWorker(AvniEncounterRepository avniEncounterRepository, AvniSubjectRepository avniSubjectRepository,
                               DqsClient dqsClient, TanuhPhotoDownloader tanuhPhotoDownloader,
                               HighRiskModelClient highRiskModelClient, TanuhErrorService tanuhErrorService,
                               ErrorRecordRepository errorRecordRepository,
                               IntegratingEntityStatusRepository integratingEntityStatusRepository,
                               TanuhContextProvider tanuhContextProvider) {
        this(avniEncounterRepository, avniSubjectRepository, dqsClient, tanuhPhotoDownloader, highRiskModelClient,
                tanuhErrorService, errorRecordRepository, integratingEntityStatusRepository, tanuhContextProvider,
                Clock.systemUTC(), PAGE_SIZE);
    }

    OralScreeningWorker(AvniEncounterRepository avniEncounterRepository, AvniSubjectRepository avniSubjectRepository,
                        DqsClient dqsClient, TanuhPhotoDownloader tanuhPhotoDownloader,
                        HighRiskModelClient highRiskModelClient, TanuhErrorService tanuhErrorService,
                        ErrorRecordRepository errorRecordRepository,
                        IntegratingEntityStatusRepository integratingEntityStatusRepository,
                        TanuhContextProvider tanuhContextProvider, Clock clock, int pageSize) {
        this.avniEncounterRepository = avniEncounterRepository;
        this.avniSubjectRepository = avniSubjectRepository;
        this.dqsClient = dqsClient;
        this.tanuhPhotoDownloader = tanuhPhotoDownloader;
        this.highRiskModelClient = highRiskModelClient;
        this.tanuhErrorService = tanuhErrorService;
        this.errorRecordRepository = errorRecordRepository;
        this.tanuhContextProvider = tanuhContextProvider;
        this.clock = clock;
        this.walker = new EncounterCursorWalker(avniEncounterRepository, integratingEntityStatusRepository, pageSize);
    }

    // Oldest first from the organisation's cursor; a version handled in an earlier run meets the eligibility checks.
    public void processNew() {
        String oralScreening = tanuhContextProvider.getConcepts().name(Concept.ORAL_SCREENING);
        walker.walk(CURSOR_ENTITY_TYPE, oralScreening, screening -> processScreening(screening, true));
    }

    // checkOpenErrorRecord is false only for the retry job (#132), which owns the screenings that have one.
    public ScreeningOutcome processScreening(GeneralEncounter s, boolean checkOpenErrorRecord) {
        String uuid = s.getUuid();
        Date listedAt = s.getLastModifiedDate();
        boolean passedQc;
        try {
            TanuhConfig config = tanuhContextProvider.get();
            TanuhConcepts concepts = tanuhContextProvider.getConcepts();
            if (!concepts.name(Concept.ORAL_SCREENING).equals(s.getEncounterType()) || !s.isCompleted() || Boolean.TRUE.equals(s.getVoided()))
                return skipped(uuid, "not a completed, live Oral Screening");
            if (config.getAvniImplUser().equalsIgnoreCase(s.getLastModifiedBy()))
                return skipped(uuid, "last changed by the job");
            if (config.getHighRiskAvniUser() != null && config.getHighRiskAvniUser().equalsIgnoreCase(s.getLastModifiedBy()))
                return skipped(uuid, "last changed by the high-risk model service");
            if (checkOpenErrorRecord && errorRecordRepository.findByAvniEntityTypeAndEntityId(AvniEntityType.GeneralEncounter, uuid) != null)
                return skipped(uuid, "waiting for a retry");
            Subject patient = avniSubjectRepository.getSubjectOrThrow(s.getSubjectId());
            if (Boolean.TRUE.equals(patient.getVoided()))
                return skipped(uuid, "patient deleted");

            // The registration goes first, so the rules that compare the screening with the patient find it. It is
            // sent with every screening: the service keeps one registration per patient, replaced on each send, and an
            // edited patient is carried by the next screening. Neither payload is logged: they carry the patient's details.
            dqsClient.submit(DqsClient.REGISTRATION, RegistrationRecord.from(s, patient, concepts));
            DqsResult quality = dqsClient.submit(DqsClient.ORAL_SCREENING, OralScreeningRecord.from(s, patient, concepts));
            if (!quality.passed()) logger.info(String.format("Screening %s failed QC: %s, %s", uuid, quality.outcomes(), quality.violations()));
            // Checked right before the write, so an edit synced while the service answered is not written over.
            if (changedSince(uuid, listedAt)) return changed(uuid);
            // The check time changes on every write. The server ignores a write that changes nothing, which would
            // leave the worker as the last editor and send the screening again on every run.
            avniEncounterRepository.patch(uuid, verdict(quality.passed(), concepts));
            logger.info(String.format("Screening %s: %s written", uuid, quality.passed() ? concepts.name(Concept.QC_PASS) : concepts.name(Concept.QC_FAIL)));
            passedQc = quality.passed();
        } catch (DqsException e) {
            return failed(uuid, TanuhErrorService.DATA_QUALITY_CALL_FAILED, e);
        } catch (Exception e) {
            return failed(uuid, TanuhErrorService.SCREENING_PROCESSING_FAILED, e);
        }
        // Handed over only once its verdict is on the screening: the hand-over is the last step, so a failure in it
        // leaves the verdict standing and only the hand-over waits for the retry (handOverAgain).
        return passedQc ? handOverOrWait(s) : ScreeningOutcome.WRITTEN;
    }

    // The retry of a screening whose verdict was written but whose hand-over failed. The verdict is read back from the
    // screening, so a screening that is no longer marked QC Pass is not handed over.
    public ScreeningOutcome handOverAgain(GeneralEncounter s) {
        TanuhConcepts concepts = tanuhContextProvider.getConcepts();
        if (!concepts.name(Concept.QC_PASS).equals(s.getObservation(concepts.name(Concept.QC_STATUS)))) {
            logger.info(String.format("Screening %s is not marked %s: not handed over", s.getUuid(), concepts.name(Concept.QC_PASS)));
            return ScreeningOutcome.SKIPPED;
        }
        return handOverOrWait(s);
    }

    private ScreeningOutcome handOverOrWait(GeneralEncounter s) {
        try {
            handOver(s, OralScreeningInputs.from(s, tanuhContextProvider.getConcepts()));
            return ScreeningOutcome.WRITTEN;
        } catch (Exception e) {
            // A photo that would not download fails the hand-over as much as the model service refusing it.
            return failed(s.getUuid(), TanuhErrorService.MODEL_CALL_FAILED, e);
        }
    }

    // A screening without photos has nothing for the model to look at, so it is not handed over.
    private void handOver(GeneralEncounter s, OralScreeningInputs inputs) throws HighRiskModelException {
        if (!inputs.hasPhotos()) {
            logger.info(String.format("Screening %s passed QC but has no photo: not sent to the high-risk model", s.getUuid()));
            return;
        }
        List<File> photos = new ArrayList<>();
        try {
            for (String url : inputs.getPhotoUrls()) photos.add(tanuhPhotoDownloader.download(url));
            highRiskModelClient.submit(s, photos);
        } finally {
            photos.forEach(File::delete);
        }
    }

    private Map<String, Object> verdict(boolean passed, TanuhConcepts concepts) {
        Map<String, Object> values = new HashMap<>();
        values.put(concepts.name(Concept.QC_STATUS), concepts.name(passed ? Concept.QC_PASS : Concept.QC_FAIL));
        values.put(concepts.name(Concept.QC_CHECK_TIME), CHECK_TIME.format(clock.instant()));
        return values;
    }

    // A PATCH merges onto the visit as it is now. If a worker's edit synced after the listing, the verdict on the
    // listed copy would be written over it, and the job's own write would then hide the edit for good.
    private boolean changedSince(String uuid, Date listedAt) {
        return !listedAt.equals(avniEncounterRepository.getGeneralEncounter(uuid).getLastModifiedDate());
    }

    private static ScreeningOutcome changed(String uuid) {
        logger.info(String.format("Screening %s changed while it was being checked: nothing written, the newer version is read next", uuid));
        return ScreeningOutcome.CHANGED;
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
