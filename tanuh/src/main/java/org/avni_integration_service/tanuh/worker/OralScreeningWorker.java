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
import org.avni_integration_service.tanuh.service.PhotoOutsideStorageException;
import org.avni_integration_service.tanuh.service.SentScreenings;
import org.avni_integration_service.tanuh.service.TanuhAlerts;
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
import java.util.OptionalInt;

// Each new or edited Oral Screening goes to the data quality service, and the QC verdict is written back to it. A
// screening that passed QC is then handed to Tanuh's high-risk model service, which works on its own time and writes its
// result to Avni itself. The job writes whether the call was made, in OCSHR Call Made. With the data quality check
// switched off (dqs_enabled false), every screening with a photo the model can read is handed over, and OCSHR Call Made
// is all the job writes.
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
    private final TanuhAlerts tanuhAlerts;
    private final SentScreenings sentScreenings;
    private final ErrorRecordRepository errorRecordRepository;
    private final TanuhContextProvider tanuhContextProvider;
    private final Clock clock;
    private final EncounterCursorWalker walker;

    @Autowired
    public OralScreeningWorker(AvniEncounterRepository avniEncounterRepository, AvniSubjectRepository avniSubjectRepository,
                               DqsClient dqsClient, TanuhPhotoDownloader tanuhPhotoDownloader,
                               HighRiskModelClient highRiskModelClient, TanuhErrorService tanuhErrorService,
                               TanuhAlerts tanuhAlerts, SentScreenings sentScreenings, ErrorRecordRepository errorRecordRepository,
                               IntegratingEntityStatusRepository integratingEntityStatusRepository,
                               TanuhContextProvider tanuhContextProvider) {
        this(avniEncounterRepository, avniSubjectRepository, dqsClient, tanuhPhotoDownloader, highRiskModelClient,
                tanuhErrorService, tanuhAlerts, sentScreenings, errorRecordRepository, integratingEntityStatusRepository, tanuhContextProvider,
                Clock.systemUTC(), PAGE_SIZE);
    }

    OralScreeningWorker(AvniEncounterRepository avniEncounterRepository, AvniSubjectRepository avniSubjectRepository,
                        DqsClient dqsClient, TanuhPhotoDownloader tanuhPhotoDownloader,
                        HighRiskModelClient highRiskModelClient, TanuhErrorService tanuhErrorService,
                        TanuhAlerts tanuhAlerts, SentScreenings sentScreenings, ErrorRecordRepository errorRecordRepository,
                        IntegratingEntityStatusRepository integratingEntityStatusRepository,
                        TanuhContextProvider tanuhContextProvider, Clock clock, int pageSize) {
        this.avniEncounterRepository = avniEncounterRepository;
        this.avniSubjectRepository = avniSubjectRepository;
        this.dqsClient = dqsClient;
        this.tanuhPhotoDownloader = tanuhPhotoDownloader;
        this.highRiskModelClient = highRiskModelClient;
        this.tanuhErrorService = tanuhErrorService;
        this.tanuhAlerts = tanuhAlerts;
        this.sentScreenings = sentScreenings;
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
        return handOverCutShort ? handOver(s, null) : check(s);
    }

    // The data quality check and the QC verdict, then the hand-over of a screening that passed and can be sent. With the
    // check off (dqs_enabled false, Adam, 17 Nov 2026), the data quality service is not asked, no verdict is written,
    // and every screening with a photo Tanuh's model can read is handed over; the job writes only OCSHR Call Made.
    private ScreeningOutcome check(GeneralEncounter s) {
        String uuid = s.getUuid();
        Date listedAt = s.getLastModifiedDate();
        boolean checkOn;
        boolean send;
        boolean wrote;
        try {
            TanuhConfig config = tanuhContextProvider.get();
            TanuhConcepts concepts = tanuhContextProvider.getConcepts();
            Subject patient = avniSubjectRepository.getSubjectOrThrow(s.getSubjectId());
            if (Boolean.TRUE.equals(patient.getVoided()))
                return skipped(uuid, "patient deleted");

            checkOn = config.isDataQualityCheckOn();
            boolean photoOutsideStorage = false;
            boolean passed = true;
            if (checkOn) {
                // The registration goes first, so the rules that compare the screening with the patient find it. It is
                // sent with every screening: the service keeps one registration per patient, replaced on each send, and an
                // edited patient is carried by the next screening. Neither payload is logged: they carry the patient's
                // details and, in the screening's, links to the photos. Those links last two minutes, so the client signs
                // them just before each post.
                dqsClient.submit(DqsClient.REGISTRATION, RegistrationRecord.from(s, patient, concepts));
                // A photo Avni will not sign (a 403) is outside the organisation's media storage. The service is not
                // asked, and the screening fails QC and is marked not sent in one write (Adam, 17 Nov 2026).
                try {
                    DqsResult quality = dqsClient.submit(DqsClient.ORAL_SCREENING, () -> OralScreeningRecord.from(s, patient, concepts, tanuhPhotoDownloader::sign));
                    DqsResult.QcVerdict qc = quality.verdict(config.getQcFailRules());
                    logVerdict(uuid, quality, qc);
                    passed = qc.passed();
                } catch (PhotoOutsideStorageException e) {
                    logger.warn(String.format("Screening %s has a photo Avni will not sign, outside the organisation's media storage: %s and not sent to the high-risk model", uuid, concepts.name(Concept.QC_FAIL)));
                    photoOutsideStorage = true;
                    passed = false;
                }
            }
            // Checked right before the write, so an edit synced while the service answered is not written over.
            if (changedSince(uuid, listedAt)) return changed(uuid);
            // The check time changes on every write. The server ignores a write that changes nothing, which would
            // leave the worker as the last editor and send the screening again on every run.
            // A screening goes to Tanuh's model once at most, and a Yes or No is final, even after an edit (Adam, 17 Nov
            // 2026). The job's own record decides, as a phone's edit can clear OCSHR Call Made on the screening: an
            // answer it holds is written back, and a send whose answer was lost is repeated as it was, whatever the edit.
            // A screening never decided that will not be sent is marked No in the same write: it failed QC, or has no
            // photo Tanuh's model can read. Each answer is kept before it is written, so the screening can be put right.
            Optional<SentScreenings.SentScreening> recordedSend = sentScreenings.find(uuid);
            Optional<Concept> recordedAnswer = recordedSend.flatMap(SentScreenings.SentScreening::callMade);
            Object callMadeOnScreen = s.getObservation(concepts.name(Concept.OCSHR_CALL_MADE));
            boolean neverDecided = callMadeOnScreen == null && recordedSend.isEmpty();
            // A send with no answer is settled by an answer a person wrote on the screening, such as one whose send kept
            // failing that they settled with Tanuh (runbook, section 8); otherwise it is repeated as it was.
            Optional<Concept> answerOnScreen = answerOn(callMadeOnScreen, concepts);
            boolean answerLost = recordedSend.isPresent() && recordedAnswer.isEmpty() && answerOnScreen.isEmpty();
            send = answerLost || (passed && neverDecided && imageUris(s, concepts).isPresent());
            boolean markedNotSent = neverDecided && !send;
            Map<String, Object> values = checkOn ? verdict(passed, concepts) : new HashMap<>();
            if (markedNotSent) {
                sentScreenings.answer(uuid, Concept.NO);
                if (photoOutsideStorage)
                    tanuhAlerts.screeningNeedsAPerson(uuid, TanuhAlerts.Reason.PHOTO_OUTSIDE_STORAGE,
                            String.format("Avni refused to sign one of its photos for the data quality service, so it is marked %s and %s %s",
                                    concepts.name(Concept.QC_FAIL), concepts.name(Concept.OCSHR_CALL_MADE), concepts.name(Concept.NO)));
                values.put(concepts.name(Concept.OCSHR_CALL_MADE), concepts.name(Concept.NO));
            } else if (recordedSend.isPresent() && recordedAnswer.isEmpty() && answerOnScreen.isPresent()) {
                logger.info(String.format("Screening %s: the %s %s written on it settles the job's record of its send", uuid,
                        concepts.name(Concept.OCSHR_CALL_MADE), callMadeOnScreen));
                sentScreenings.answer(uuid, answerOnScreen.get());
            } else if (callMadeOnScreen == null && recordedAnswer.isPresent()) {
                logger.warn(String.format("Screening %s lost its %s in an edit: %s is written back from the job's record and it is not sent again",
                        uuid, concepts.name(Concept.OCSHR_CALL_MADE), concepts.name(recordedAnswer.get())));
                values.put(concepts.name(Concept.OCSHR_CALL_MADE), concepts.name(recordedAnswer.get()));
            }
            // With the check off there is nothing to write when the screening is already marked, or is about to be sent.
            wrote = !values.isEmpty();
            if (wrote) {
                avniEncounterRepository.patch(uuid, values);
                if (checkOn)
                    logger.info(String.format("Screening %s: %s written", uuid, passed ? concepts.name(Concept.QC_PASS) : concepts.name(Concept.QC_FAIL)));
                else
                    logger.info(String.format("Screening %s: %s %s written", uuid, concepts.name(Concept.OCSHR_CALL_MADE), values.get(concepts.name(Concept.OCSHR_CALL_MADE))));
            }
            if (passed && markedNotSent)
                logger.warn(String.format("Screening %s %s but has no photo, or a photo outside S3: marked not sent to the high-risk model", uuid,
                        checkOn ? "passed QC" : "is not checked for quality"));
        } catch (DqsException e) {
            return failed(uuid, TanuhErrorService.DATA_QUALITY_CALL_FAILED, e);
        } catch (Exception e) {
            return failed(uuid, TanuhErrorService.SCREENING_PROCESSING_FAILED, e);
        }
        // Handed over only once its verdict is on the screening: the hand-over is the last step, so a failure in it
        // leaves the verdict standing and only the hand-over waits for the retry (handOverAgain). With the check off
        // nothing was written before the hand-over, so the listed version is what its answer must still find.
        if (send) return handOver(s, checkOn ? null : listedAt);
        return wrote ? ScreeningOutcome.WRITTEN : skipped(uuid, "already marked, and the data quality check is off");
    }

    // The retry of a screening whose verdict was written but whose hand-over failed. The screening is read again for
    // the retry, so one already marked sent or not sent, or no longer marked QC Pass with no send of its own to settle,
    // is not handed over.
    public ScreeningOutcome handOverAgain(GeneralEncounter s) {
        TanuhConcepts concepts = tanuhContextProvider.getConcepts();
        if (!awaitsHandOver(s, concepts)) {
            logger.info(String.format("Screening %s has %s set, or is not marked %s and was never sent: not handed over", s.getUuid(),
                    concepts.name(Concept.OCSHR_CALL_MADE), concepts.name(Concept.QC_PASS)));
            return ScreeningOutcome.SKIPPED;
        }
        return handOver(s, null);
    }

    // The hand-over of a screening just checked, of one whose hand-over was cut short, and of one whose hand-over is
    // retried. OCSHR Call Made says whether the call was made (Adam, 17 Nov 2026): Yes once Tanuh's service answers for
    // the case, whether it took it, refused it, or already holds it with other photos, and No without a call when Avni
    // will not sign one of its photos. A call that fails or gets no answer writes nothing, and the same request goes
    // again. A refusal, a case held with other photos and a photo Avni will not sign are reported. listedAt is the
    // version's time when nothing was written to it before the hand-over, and null otherwise (see writeCallMade).
    private ScreeningOutcome handOver(GeneralEncounter s, Date listedAt) {
        String uuid = s.getUuid();
        try {
            Optional<SentScreenings.SentScreening> recordedSend = sentScreenings.find(uuid);
            Optional<Concept> recordedAnswer = recordedSend.flatMap(SentScreenings.SentScreening::callMade);
            // Settled before: nothing is sent, and the answer an edit cleared is written back.
            if (recordedAnswer.isPresent()) return writeCallMade(uuid, recordedAnswer.get(), listedAt);
            Optional<List<String>> firstSend = recordedSend.flatMap(SentScreenings.SentScreening::imageUris);
            OralScreeningInputs inputs = OralScreeningInputs.from(s, tanuhContextProvider.getConcepts());
            List<String> uris;
            if (firstSend.isPresent()) {
                // A send whose answer was lost: Tanuh may hold it, so it is repeated as it was, whatever an edit changed
                // since, and Tanuh answers the repeat as the same case. An edit's photos never reach the model. The
                // phone's verdict is not recorded, so the repeat carries the answer the screening holds now.
                uris = firstSend.get();
            } else {
                Optional<List<String>> imageUris = inputs.hasPhotos() ? S3Uris.all(inputs.getPhotoUrls()) : Optional.empty();
                // Met only when the hand-over was cut short or is retried: a screening just checked has its No in its verdict.
                if (imageUris.isEmpty()) {
                    logger.warn(String.format("Screening %s has no photo, or a photo outside S3: not sent to the high-risk model", uuid));
                    sentScreenings.answer(uuid, Concept.NO);
                    return writeCallMade(uuid, Concept.NO, listedAt);
                }
                uris = imageUris.get();
                // Sending more would earn a refusal, and a refused case is never scored, so the first ones in the form's
                // order go (Adam, 17 Nov 2026). A case cannot change once sent: should Tanuh raise the limit, these keep the
                // photos they were sent with.
                if (uris.size() > MAX_IMAGES) {
                    logger.warn(String.format("Screening %s has %d photos and Tanuh's service takes at most %d per case: the first %d are sent", uuid, uris.size(), MAX_IMAGES, MAX_IMAGES));
                    uris = List.copyOf(uris.subList(0, MAX_IMAGES));
                }
                OptionalInt refusedPhoto = photoAvniRefuses(inputs.getPhotoUrls());
                if (refusedPhoto.isPresent()) {
                    logger.warn(String.format("Screening %s: Avni will not sign photo %d, which is outside the organisation's media storage: marked not sent to the high-risk model", uuid, refusedPhoto.getAsInt()));
                    sentScreenings.answer(uuid, Concept.NO);
                    tanuhAlerts.screeningNeedsAPerson(uuid, TanuhAlerts.Reason.PHOTO_OUTSIDE_STORAGE,
                            String.format("Avni refused to sign photo %d before the hand-over", refusedPhoto.getAsInt()));
                    return writeCallMade(uuid, Concept.NO, listedAt);
                }
                // Kept before the send, so a send whose answer is lost is repeated with these photos and no others.
                sentScreenings.attempt(uuid, uris);
            }
            Submission submission = highRiskModelClient.submit(uuid, uris, inputs.getEdgeVerdict());
            switch (submission.outcome()) {
                case ACCEPTED -> {
                    sentScreenings.answer(uuid, Concept.YES);
                    return writeCallMade(uuid, Concept.YES, listedAt);
                }
                case REFUSED -> {
                    logger.warn(String.format("Screening %s was refused by the high-risk model service, which will not score it; the call was made, so it is marked sent: %s", uuid, submission.detail()));
                    return sentButNotScored(uuid, TanuhAlerts.Reason.REFUSED, submission.detail(), listedAt);
                }
                case CONFLICT -> {
                    logger.warn(String.format("Screening %s is held by the high-risk model service with other photos, which are the ones it scores; the call was made, so it is marked sent: %s", uuid, submission.detail()));
                    return sentButNotScored(uuid, TanuhAlerts.Reason.HELD_WITH_OTHER_PHOTOS, submission.detail(), listedAt);
                }
                default -> throw new HighRiskModelException("The high-risk model service gave an answer the job does not know: " + submission.outcome());
            }
        } catch (Exception e) {
            // The model client logs nothing when it throws, so this is where the reason is logged and recorded. An
            // answer with no outcome lands here too, as a failed call: nothing is written and the same request goes again.
            return failed(uuid, TanuhErrorService.MODEL_CALL_FAILED, e);
        }
    }

    // Tanuh's service answered for the case without taking these photos, so it will not score them, but the call was
    // made: Yes is kept and written, the screening is never sent again, and a person is told so they can take it up with
    // Tanuh's team. The answer is kept and reported before the write, so a write that does not land loses neither.
    private ScreeningOutcome sentButNotScored(String uuid, TanuhAlerts.Reason reason, String detail, Date listedAt) {
        sentScreenings.answer(uuid, Concept.YES);
        tanuhAlerts.screeningNeedsAPerson(uuid, reason, detail);
        return writeCallMade(uuid, Concept.YES, listedAt);
    }

    // Avni signs only an address in the organisation's own media storage and answers 403 for any other, so another
    // organisation's photo written into this screening's answer is never sent. This gives the first photo it refuses, if
    // any. Any other failure to sign is no verdict on the photo, so it throws and the screening waits. The signed link is
    // not used: nothing is downloaded.
    private OptionalInt photoAvniRefuses(List<String> photoUrls) throws HighRiskModelException {
        for (int i = 0; i < photoUrls.size(); i++) {
            try {
                tanuhPhotoDownloader.sign(photoUrls.get(i));
            } catch (PhotoOutsideStorageException e) {
                return OptionalInt.of(i + 1);
            } catch (RuntimeException e) {
                throw new HighRiskModelException(String.format("Avni could not sign photo %d, so the screening waits: %s", i + 1, e.getMessage()), e);
            }
        }
        return OptionalInt.empty();
    }

    // Written only while the job or Tanuh's service changed the screening last, or, when the job wrote nothing to it
    // before the hand-over (the data quality check off), while it is still the version listed at listedAt. Anyone else's
    // edit since is left as it is, and read on its next listing. Tanuh's service counts because its result can land
    // before this write. After a job's own write the PATCH reply's time cannot be the baseline, as Avni stamps the change
    // when the save is flushed, after the reply is built; a listed version's time is the server's own.
    private ScreeningOutcome writeCallMade(String uuid, Concept answer, Date listedAt) {
        TanuhConcepts concepts = tanuhContextProvider.getConcepts();
        String callMade = concepts.name(Concept.OCSHR_CALL_MADE);
        GeneralEncounter now = avniEncounterRepository.getGeneralEncounter(uuid);
        boolean unchangedSinceListed = listedAt != null && listedAt.equals(now.getLastModifiedDate());
        if (!unchangedSinceListed && !lastChangedByJobOrModel(now, tanuhContextProvider.get())) {
            if (answer == Concept.YES)
                logger.warn(String.format("Screening %s changed while it was handed over: the call was made, but %s is not written; the newer version is read next", uuid, callMade));
            else
                logger.info(String.format("Screening %s changed while it was handed over: %s not written, the newer version is read next", uuid, callMade));
            return ScreeningOutcome.CHANGED;
        }
        avniEncounterRepository.patch(uuid, Map.of(callMade, concepts.name(answer)));
        logger.info(String.format("Screening %s: %s %s written", uuid, callMade, concepts.name(answer)));
        return ScreeningOutcome.WRITTEN;
    }

    // OCSHR Call Made as written on the screening, read as the job's YES or NO; empty for no answer or any other value.
    private static Optional<Concept> answerOn(Object callMade, TanuhConcepts concepts) {
        if (concepts.name(Concept.YES).equals(callMade)) return Optional.of(Concept.YES);
        if (concepts.name(Concept.NO).equals(callMade)) return Optional.of(Concept.NO);
        return Optional.empty();
    }

    // The job's own writes, and Tanuh's result when high_risk_avni_user names the user it writes as.
    private static boolean lastChangedByJobOrModel(GeneralEncounter s, TanuhConfig config) {
        String by = s.getLastModifiedBy();
        return config.getAvniImplUser().equalsIgnoreCase(by)
                || (config.getHighRiskAvniUser() != null && config.getHighRiskAvniUser().equalsIgnoreCase(by));
    }

    // OCSHR Call Made is empty on the screening, and it is marked QC Pass or the job's record holds a send or an answer
    // for it: a send whose answer was lost is settled, and an answer an edit cleared is written back, whatever the verdict.
    private boolean awaitsHandOver(GeneralEncounter s, TanuhConcepts concepts) {
        if (s.getObservation(concepts.name(Concept.OCSHR_CALL_MADE)) != null) return false;
        return concepts.name(Concept.QC_PASS).equals(s.getObservation(concepts.name(Concept.QC_STATUS)))
                || sentScreenings.find(s.getUuid()).isPresent();
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
