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
import org.avni_integration_service.tanuh.model.S3Uris;
import org.avni_integration_service.tanuh.model.Submission;
import org.avni_integration_service.tanuh.service.TanuhErrorService;
import org.avni_integration_service.tanuh.service.TanuhPhotoDownloader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Each new or edited Oral Screening goes to the data quality service, and the QC verdict is written back to it. A
// screening that passed QC is then handed to Tanuh's high-risk model service, which works on its own time and writes its
// result to Avni itself. The job writes only whether the screening was sent, in OCSHR Call Made.
@Component
public class OralScreeningWorker {
    public static final String CURSOR_ENTITY_TYPE = "TanuhOralScreening";
    static final int PAGE_SIZE = EncounterCursorWalker.PAGE_SIZE;
    // Tanuh's service takes 1 to 13 photos per case (its intake guide, section 8).
    static final int MAX_IMAGES = 13;
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
        boolean handOverCutShort;
        try {
            TanuhConfig config = tanuhContextProvider.get();
            TanuhConcepts concepts = tanuhContextProvider.getConcepts();
            if (!concepts.name(Concept.ORAL_SCREENING).equals(s.getEncounterType()) || !s.isCompleted() || Boolean.TRUE.equals(s.getVoided()))
                return skipped(uuid, "not a completed, live Oral Screening");
            // A version the job or Tanuh's service wrote has been checked, so it is left alone, unless it is marked QC
            // Pass with OCSHR Call Made still empty. Then its hand-over was cut short: the job stopped before writing Call
            // Made, or Tanuh's result landed after a send that timed out. It is handed over without a second check.
            handOverCutShort = lastChangedByJobOrModel(s, config);
            if (handOverCutShort && !awaitsHandOver(s, concepts))
                return skipped(uuid, "last changed by the job or the high-risk model service");
            if (checkOpenErrorRecord && errorRecordRepository.findByAvniEntityTypeAndEntityId(AvniEntityType.GeneralEncounter, uuid) != null)
                return skipped(uuid, "waiting for a retry");
            // The listed copy is sent only if it is still the screening as it is now: a page can take minutes, and a
            // worker's edit since, or the retry job finishing the same hand-over, would otherwise send a stale copy.
            if (handOverCutShort && changedSince(uuid, s.getLastModifiedDate())) return changed(uuid);
        } catch (Exception e) {
            return failed(uuid, TanuhErrorService.SCREENING_PROCESSING_FAILED, e);
        }
        return handOverCutShort ? handOver(s) : check(s);
    }

    // The data quality check and the QC verdict, then the hand-over of a screening that passed and can be sent.
    private ScreeningOutcome check(GeneralEncounter s) {
        String uuid = s.getUuid();
        Date listedAt = s.getLastModifiedDate();
        boolean send;
        try {
            TanuhConfig config = tanuhContextProvider.get();
            TanuhConcepts concepts = tanuhContextProvider.getConcepts();
            Subject patient = avniSubjectRepository.getSubjectOrThrow(s.getSubjectId());
            if (Boolean.TRUE.equals(patient.getVoided()))
                return skipped(uuid, "patient deleted");

            // The registration goes first, so the rules that compare the screening with the patient find it. It is
            // sent with every screening: the service keeps one registration per patient, replaced on each send, and an
            // edited patient is carried by the next screening. Neither payload is logged: they carry the patient's details
            // and, in the screening's, links to the photos. Those links last two minutes, so the client signs them just
            // before each post.
            dqsClient.submit(DqsClient.REGISTRATION, RegistrationRecord.from(s, patient, concepts));
            DqsResult quality = dqsClient.submit(DqsClient.ORAL_SCREENING, () -> OralScreeningRecord.from(s, patient, concepts, tanuhPhotoDownloader::sign));
            DqsResult.QcVerdict qc = quality.verdict(config.getQcFailRules());
            logVerdict(uuid, quality, qc);
            boolean passed = qc.passed();
            // Checked right before the write, so an edit synced while the service answered is not written over.
            if (changedSince(uuid, listedAt)) return changed(uuid);
            // The check time changes on every write. The server ignores a write that changes nothing, which would
            // leave the worker as the last editor and send the screening again on every run.
            // A screening is sent only while OCSHR Call Made is empty, and a Yes or No is final. One that will not be
            // sent is marked No in the same write: it failed QC, or has no photo Tanuh's model can read. One already
            // marked keeps its answer.
            boolean notYetSent = s.getObservation(concepts.name(Concept.OCSHR_CALL_MADE)) == null;
            send = passed && notYetSent && imageUris(s, concepts).isPresent();
            Map<String, Object> values = verdict(passed, concepts);
            if (notYetSent && !send) values.put(concepts.name(Concept.OCSHR_CALL_MADE), concepts.name(Concept.NO));
            avniEncounterRepository.patch(uuid, values);
            logger.info(String.format("Screening %s: %s written", uuid, passed ? concepts.name(Concept.QC_PASS) : concepts.name(Concept.QC_FAIL)));
            if (passed && notYetSent && !send)
                logger.warn(String.format("Screening %s passed QC but has no photo, or a photo outside S3: marked not sent to the high-risk model", uuid));
        } catch (DqsException e) {
            return failed(uuid, TanuhErrorService.DATA_QUALITY_CALL_FAILED, e);
        } catch (Exception e) {
            return failed(uuid, TanuhErrorService.SCREENING_PROCESSING_FAILED, e);
        }
        // Handed over only once its verdict is on the screening: the hand-over is the last step, so a failure in it
        // leaves the verdict standing and only the hand-over waits for the retry (handOverAgain).
        return send ? handOver(s) : ScreeningOutcome.WRITTEN;
    }

    // The retry of a screening whose verdict was written but whose hand-over failed. The screening is read again for
    // the retry, so one no longer marked QC Pass, or already marked sent or not sent, is not handed over.
    public ScreeningOutcome handOverAgain(GeneralEncounter s) {
        TanuhConcepts concepts = tanuhContextProvider.getConcepts();
        if (!awaitsHandOver(s, concepts)) {
            logger.info(String.format("Screening %s is not marked %s with %s empty: not handed over", s.getUuid(),
                    concepts.name(Concept.QC_PASS), concepts.name(Concept.OCSHR_CALL_MADE)));
            return ScreeningOutcome.SKIPPED;
        }
        return handOver(s);
    }

    // The hand-over of a screening just checked, of one whose hand-over was cut short, and of one whose hand-over is
    // retried. Tanuh's answer goes on the screening as OCSHR Call Made: Yes when the service took it, No when it refused
    // it. A screening the service holds with other photos is parked for a person, and nothing is written.
    private ScreeningOutcome handOver(GeneralEncounter s) {
        String uuid = s.getUuid();
        Submission submission;
        try {
            OralScreeningInputs inputs = OralScreeningInputs.from(s, tanuhContextProvider.getConcepts());
            Optional<List<String>> imageUris = inputs.hasPhotos() ? S3Uris.all(inputs.getPhotoUrls()) : Optional.empty();
            // Met only when the hand-over was cut short or is retried: a screening just checked has its No in its verdict.
            if (imageUris.isEmpty()) {
                logger.warn(String.format("Screening %s has no photo, or a photo outside S3: not sent to the high-risk model", uuid));
                return writeCallMade(uuid, Concept.NO);
            }
            List<String> uris = imageUris.get();
            // Sending more would earn a refusal and a No for good, so the screening waits until the limit is settled.
            if (uris.size() > MAX_IMAGES)
                throw new HighRiskModelException(String.format("%d photos, and Tanuh's service takes at most %d per case, so it waits until that is settled", uris.size(), MAX_IMAGES));
            confirmAvniSigns(inputs.getPhotoUrls());
            submission = highRiskModelClient.submit(uuid, uris);
            switch (submission.outcome()) {
                case ACCEPTED -> {
                    return writeCallMade(uuid, Concept.YES);
                }
                case REFUSED -> {
                    logger.warn(String.format("Screening %s refused by the high-risk model service, so it is marked not sent: %s", uuid, submission.detail()));
                    return writeCallMade(uuid, Concept.NO);
                }
                case CONFLICT -> {
                }
                default -> throw new HighRiskModelException("The high-risk model service gave an answer the job does not know: " + submission.outcome());
            }
        } catch (Exception e) {
            // The model client logs nothing when it throws, so this is where the reason is logged and recorded. An
            // answer with no outcome lands here too, as a failed call to retry, never as a parked screening.
            return failed(uuid, TanuhErrorService.MODEL_CALL_FAILED, e);
        }
        // CONFLICT, recorded outside the catch above: a missing error type stops the run, as it does in failed().
        return parked(uuid, submission.detail());
    }

    // Avni signs only an address in the organisation's own media folder, so another organisation's photo written into
    // this screening's answer is never sent. Nothing is downloaded: the signed link is not used.
    private void confirmAvniSigns(List<String> photoUrls) throws HighRiskModelException {
        for (int i = 0; i < photoUrls.size(); i++) {
            try {
                tanuhPhotoDownloader.sign(photoUrls.get(i));
            } catch (RuntimeException e) {
                throw new HighRiskModelException(String.format("Avni would not sign photo %d, so the screening is not sent; only the organisation's own photos go to Tanuh: %s", i + 1, e.getMessage()), e);
            }
        }
    }

    // Written only while the job or Tanuh's service changed the screening last: anyone else's edit since is left as it
    // is, and read on its next listing. Tanuh's service counts because its result can land before this write. The PATCH
    // reply's time cannot be the baseline, as Avni stamps the change when the save is flushed, after the reply is built.
    private ScreeningOutcome writeCallMade(String uuid, Concept answer) {
        TanuhConcepts concepts = tanuhContextProvider.getConcepts();
        String callMade = concepts.name(Concept.OCSHR_CALL_MADE);
        if (!lastChangedByJobOrModel(avniEncounterRepository.getGeneralEncounter(uuid), tanuhContextProvider.get())) {
            if (answer == Concept.YES)
                logger.warn(String.format("Screening %s changed while it was handed over: Tanuh's service took it, but %s is not written; the newer version is read next", uuid, callMade));
            else
                logger.info(String.format("Screening %s changed while it was handed over: %s not written, the newer version is read next", uuid, callMade));
            return ScreeningOutcome.CHANGED;
        }
        avniEncounterRepository.patch(uuid, Map.of(callMade, concepts.name(answer)));
        logger.info(String.format("Screening %s: %s %s written", uuid, callMade, concepts.name(answer)));
        return ScreeningOutcome.WRITTEN;
    }

    // Tanuh's service holds this screening with other photos, so sending it again cannot help. It waits in an error
    // record of a type the retry job skips, for a person to settle with Tanuh.
    private ScreeningOutcome parked(String uuid, String detail) {
        logger.warn(String.format("Screening %s is held by the high-risk model service with other photos: parked for a person: %s", uuid, detail));
        tanuhErrorService.errorOccurred(uuid, TanuhErrorService.MODEL_CASE_CONFLICT,
                String.format("The high-risk model service holds this screening with other photos: %s", detail));
        return ScreeningOutcome.PARKED;
    }

    // The job's own writes, and Tanuh's result when high_risk_avni_user names the user it writes as.
    private static boolean lastChangedByJobOrModel(GeneralEncounter s, TanuhConfig config) {
        String by = s.getLastModifiedBy();
        return config.getAvniImplUser().equalsIgnoreCase(by)
                || (config.getHighRiskAvniUser() != null && config.getHighRiskAvniUser().equalsIgnoreCase(by));
    }

    // Checked and marked QC Pass, and not yet marked sent or not sent.
    private static boolean awaitsHandOver(GeneralEncounter s, TanuhConcepts concepts) {
        return concepts.name(Concept.QC_PASS).equals(s.getObservation(concepts.name(Concept.QC_STATUS)))
                && s.getObservation(concepts.name(Concept.OCSHR_CALL_MADE)) == null;
    }

    // The photos as Tanuh's model reads them: S3 locations, in order and each once. Empty when there is none, or when
    // any has no S3 location, since sending the rest would hide the photo left out.
    private static Optional<List<String>> imageUris(GeneralEncounter s, TanuhConcepts concepts) {
        OralScreeningInputs inputs = OralScreeningInputs.from(s, concepts);
        return inputs.hasPhotos() ? S3Uris.all(inputs.getPhotoUrls()) : Optional.empty();
    }

    // Whether a screening passes is decided here, from the rule outcomes the service returned: see DqsResult.passed.
    private static void logVerdict(String uuid, DqsResult quality, DqsResult.QcVerdict qc) {
        if (!quality.scored()) {
            logger.info(String.format("Screening %s failed QC: the data quality service could not read it: %s", uuid, quality.violations()));
            return;
        }
        // A listed rule the service did not run can never fail a screening, so the setting is not doing what it says.
        if (!qc.unreported().isEmpty())
            logger.warn(String.format("Screening %s: qc_fail_rules lists %s, which the data quality service gave no outcome for: the id is misspelt, or the tenant's rules no longer run it", uuid, qc.unreported()));
        if (!qc.passed())
            logger.info(String.format("Screening %s failed QC on %s; all failed rules %s, violations %s", uuid, qc.failingQc(), qc.failed(), quality.violations()));
        else if (!qc.failed().isEmpty())
            logger.info(String.format("Screening %s passed QC; failed rules not in qc_fail_rules: %s", uuid, qc.failed()));
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
