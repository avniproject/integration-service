package org.avni_integration_service.tanuh.worker;

import org.avni_integration_service.tanuh.domain.TanuhConceptsFixture;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.avni.repository.AvniSubjectRepository;
import org.avni_integration_service.integration_data.domain.AvniEntityType;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfig;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.avni_integration_service.integration_data.domain.error.ErrorRecord;
import org.avni_integration_service.integration_data.repository.ErrorRecordRepository;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.avni_integration_service.tanuh.config.TanuhConfig;
import org.avni_integration_service.tanuh.config.TanuhContextProvider;
import org.avni_integration_service.tanuh.dqs.DqsClient;
import org.avni_integration_service.tanuh.dqs.DqsException;
import org.avni_integration_service.tanuh.dqs.DqsResult;
import org.avni_integration_service.tanuh.dqs.DqsResult.RuleOutcome;
import org.avni_integration_service.tanuh.model.HighRiskModelClient;
import org.avni_integration_service.tanuh.model.HighRiskModelException;
import org.avni_integration_service.tanuh.model.Submission;
import org.avni_integration_service.tanuh.domain.TanuhConcepts.Concept;
import org.avni_integration_service.tanuh.service.PhotoOutsideStorageException;
import org.avni_integration_service.tanuh.service.SentScreenings;
import org.avni_integration_service.tanuh.service.TanuhAlerts;
import org.avni_integration_service.tanuh.service.TanuhErrorService;
import org.avni_integration_service.tanuh.service.TanuhPhotoDownloader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProcessScreeningTest {
    static final String JOB_USER = "integration@tanuh_uat_local";
    static final String HIGH_RISK_USER = "high-risk@tanuh_uat_local";
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T09:15:00.123Z"), ZoneOffset.UTC);
    static final String CHECK_TIME = "2026-10-07T09:15:00.123Z";
    static final String PHOTOS = "Take photos of all lesions and 1 photo without lesion";

    @Mock
    private AvniSubjectRepository subjects;
    @Mock
    private TanuhPhotoDownloader downloader;
    @Mock
    private HighRiskModelClient model;
    @Mock
    private DqsClient dqs;
    @Mock
    private TanuhErrorService errors;
    @Mock
    private TanuhAlerts alerts;
    @Mock
    private ErrorRecordRepository errorRecords;
    @Mock
    private IntegratingEntityStatusRepository statuses;
    private final TanuhContextProvider context = new TanuhContextProvider();
    private FakeAvniEncounterRepository encounters;
    private final FakeSentScreenings sent = new FakeSentScreenings();
    private OralScreeningWorker worker;

    @BeforeEach
    public void setUp() throws Exception {
        IntegrationSystemConfig user = new IntegrationSystemConfig();
        user.setKey("avni_user");
        user.setValue(JOB_USER);
        IntegrationSystemConfig highRiskUser = new IntegrationSystemConfig();
        highRiskUser.setKey("high_risk_avni_user");
        highRiskUser.setValue(HIGH_RISK_USER);
        IntegrationSystem system = new IntegrationSystem();
        system.setId(15);
        ReflectionTestUtils.setField(system, "name", "tanuh_uat_local");
        context.set(new TanuhConfig(new IntegrationSystemConfigCollection(List.of(user, highRiskUser)), system));
        context.setConcepts(TanuhConceptsFixture.defaults());
        encounters = new FakeAvniEncounterRepository(JOB_USER, Date.from(Instant.parse("2026-10-07T00:00:00Z")));
        worker = new OralScreeningWorker(encounters, subjects, dqs, downloader, model, errors, alerts, sent, errorRecords, statuses, context, CLOCK, 10);
        lenient().when(subjects.getSubjectOrThrow(anyString())).thenReturn(patient(false));
        // The patient's registration goes ahead of every screening; the service stores it and the tests leave it be.
        lenient().when(dqs.submit(eq(DqsClient.REGISTRATION), anyMap())).thenReturn(passed());
        lenient().when(downloader.sign(anyString())).thenAnswer(invocation -> invocation.getArgument(0) + "?X-Amz-Signature=signed");
        // Tanuh's model service takes every screening unless a test says otherwise.
        lenient().when(model.submit(anyString(), anyList())).thenReturn(new Submission(Submission.Outcome.ACCEPTED, "{}"));
    }

    @AfterEach
    public void tearDown() {
        TanuhContextProvider.clear();
    }

    private static Subject patient(boolean voided) {
        Subject subject = new Subject();
        subject.setVoided(voided);
        return subject;
    }

    private static Map<String, Object> row(String image, String suspicious) {
        Map<String, Object> row = new HashMap<>();
        row.put("Oral Image", image);
        row.put("Suspicious Lesion?", suspicious);
        return row;
    }

    private GeneralEncounter screeningWithPhotos(String uuid, String... suspiciousPerPhoto) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < suspiciousPerPhoto.length; i++)
            rows.add(row("https://s3.ap-south-1.amazonaws.com/prod-user-media/tanuh_uat/" + uuid + "-" + i + ".jpg", suspiciousPerPhoto[i]));
        return encounters.add(uuid, Map.of(PHOTOS, rows));
    }

    static DqsResult passed() {
        return new DqsResult(200, true, List.of(), List.of(new RuleOutcome("ST-001", "pass"), new RuleOutcome("QC-030", "pass")));
    }

    // QC-030, unusable images, is one of the default qc_fail_rules.
    static DqsResult failedRule() {
        return new DqsResult(200, true, List.of(), List.of(new RuleOutcome("ST-001", "pass"), new RuleOutcome("QC-030", "fail")));
    }

    // SL-001, no location, is not.
    static DqsResult failedRuleNotFailingQc() {
        return new DqsResult(200, true, List.of(), List.of(new RuleOutcome("ST-001", "pass"), new RuleOutcome("SL-001", "fail")));
    }

    static Map<String, Object> verdict(String qcStatus) {
        return Map.of("QC status", qcStatus, "QC check time", CHECK_TIME);
    }

    // A screening that will not be sent is marked so in the same PATCH as its verdict.
    static Map<String, Object> notSent(String qcStatus) {
        Map<String, Object> values = new HashMap<>(verdict(qcStatus));
        values.put("OCSHR Call Made", "No");
        return values;
    }

    // A screening that fails QC is not sent, and the job writes that down in the same PATCH.
    static Map<String, Object> failVerdict() {
        return notSent("QC Fail");
    }

    // Written once Tanuh's model service has answered.
    static Map<String, Object> callMade(String answer) {
        return Map.of("OCSHR Call Made", answer);
    }

    // Checked and marked QC Pass, then last changed by this user before Tanuh's answer was written down.
    private GeneralEncounter handOverCutShort(String uuid, String lastChangedBy) {
        GeneralEncounter s = screeningWithPhotos(uuid, "No");
        s.addObservation("QC status", "QC Pass");
        s.addObservation("QC check time", CHECK_TIME);
        encounters.touch(s, lastChangedBy);
        return s;
    }

    // The real client builds the screening's record, signing each photo, before it posts; this one does the same.
    private void dqsBuildsTheRecord() {
        when(dqs.submit(eq(DqsClient.ORAL_SCREENING), any(Supplier.class))).thenAnswer(invocation -> {
            ((Supplier<?>) invocation.getArgument(1)).get();
            return passed();
        });
    }

    private void dqsSays(DqsResult result) {
        when(dqs.submit(eq(DqsClient.ORAL_SCREENING), any(Supplier.class))).thenReturn(result);
    }

    @Test
    public void aScreeningThatPassesQcIsHandedToTheModelAndMarkedPass() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No", "Yes");
        dqsSays(passed());

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        assertEquals(List.of(verdict("QC Pass"), callMade("Yes")), encounters.patchBodies);
        ArgumentCaptor<Supplier<Map<String, Object>>> record = ArgumentCaptor.forClass(Supplier.class);
        verify(dqs).submit(eq(DqsClient.ORAL_SCREENING), record.capture());
        assertEquals("s1", record.getValue().get().get("record_id"));
        assertEquals(2, record.getValue().get().get("image_count"));
        assertEquals(List.of("https://s3.ap-south-1.amazonaws.com/prod-user-media/tanuh_uat/s1-0.jpg?X-Amz-Signature=signed",
                        "https://s3.ap-south-1.amazonaws.com/prod-user-media/tanuh_uat/s1-1.jpg?X-Amz-Signature=signed"),
                record.getValue().get().get("image_urls"));
        verify(model).submit(eq("s1"), eq(List.of("s3://prod-user-media/tanuh_uat/s1-0.jpg", "s3://prod-user-media/tanuh_uat/s1-1.jpg")));
        verifyNoInteractions(errors);
    }

    @Test
    public void aScreeningARuleFailsIsMarkedFailAndNotHandedToTheModel() {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        dqsSays(failedRule());

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        assertEquals(List.of(failVerdict()), encounters.patchBodies);
        verifyNoInteractions(model, errors);
    }

    @Test
    public void aFailedRuleNotInQcFailRulesIsLoggedAndTheScreeningPasses() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        dqsSays(failedRuleNotFailingQc());

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        assertEquals(List.of(verdict("QC Pass"), callMade("Yes")), encounters.patchBodies);
        verify(model).submit(eq("s1"), eq(List.of("s3://prod-user-media/tanuh_uat/s1-0.jpg")));
    }

    @Test
    public void theRulesThatFailQcAreTheOrganisationsChoice() {
        IntegrationSystemConfig user = new IntegrationSystemConfig();
        user.setKey("avni_user");
        user.setValue(JOB_USER);
        IntegrationSystemConfig failRules = new IntegrationSystemConfig();
        failRules.setKey("qc_fail_rules");
        failRules.setValue(" SL-001 , ST-001 ");
        IntegrationSystem system = new IntegrationSystem();
        system.setId(15);
        ReflectionTestUtils.setField(system, "name", "tanuh_uat_local");
        context.set(new TanuhConfig(new IntegrationSystemConfigCollection(List.of(user, failRules)), system));
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        dqsSays(failedRuleNotFailingQc());

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        assertEquals(List.of(failVerdict()), encounters.patchBodies);
        verifyNoInteractions(model);
    }

    @Test
    public void anUnreadableScreeningIsMarkedFail() {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        dqsSays(new DqsResult(422, false, List.of("error IDENTITY_FIELD_MISSING: absent"), List.of()));

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        assertEquals(List.of(failVerdict()), encounters.patchBodies);
        verifyNoInteractions(model);
    }

    // Tanuh's model has nothing to look at, so the screening is marked not sent in the same PATCH as its verdict.
    @Test
    public void aScreeningWithoutPhotosIsMarkedNotSent() {
        GeneralEncounter s = encounters.add("s1", Map.of("Able to Open Mouth?", "No"));
        dqsSays(passed());

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        assertEquals(List.of(notSent("QC Pass")), encounters.patchBodies);
        verifyNoInteractions(model);
    }

    // Tanuh's model reads photos from S3 only, and sending the rest would hide the photo left out.
    @Test
    public void aPhotoOutsideS3MarksTheScreeningNotSent() {
        GeneralEncounter s = encounters.add("s1", Map.of(PHOTOS, List.of(
                row("https://s3.ap-south-1.amazonaws.com/prod-user-media/tanuh_uat/s1-0.jpg", "No"),
                row("minio://avni-user-media/t/s1-1.jpg", "No"))));
        dqsSays(passed());

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        assertEquals(List.of(notSent("QC Pass")), encounters.patchBodies);
        verifyNoInteractions(model);
    }

    @Test
    public void notCompletedVoidedOrAnotherTypeIsSkipped() {
        GeneralEncounter scheduled = screeningWithPhotos("planned", "No");
        scheduled.getProperties().remove("Encounter date time");
        GeneralEncounter voided = screeningWithPhotos("voided", "No");
        voided.setVoided(true);
        GeneralEncounter review = screeningWithPhotos("review", "No");
        review.setEncounterType("Clinician Review Form");

        for (GeneralEncounter s : List.of(scheduled, voided, review))
            assertEquals(ScreeningOutcome.SKIPPED, worker.processScreening(s, true));
        assertTrue(encounters.patchBodies.isEmpty());
        verifyNoInteractions(dqs, model, subjects);
    }

    @Test
    public void aDeletedPatientIsSkipped() {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        when(subjects.getSubjectOrThrow("patient-of-s1")).thenReturn(patient(true));

        assertEquals(ScreeningOutcome.SKIPPED, worker.processScreening(s, true));
        assertTrue(encounters.patchBodies.isEmpty());
        verifyNoInteractions(dqs, model);
    }

    @Test
    public void lastChangedByTheJobIsSkippedWhateverTheCase() {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        encounters.touch(s, "INTEGRATION@Tanuh_UAT_Local");

        assertEquals(ScreeningOutcome.SKIPPED, worker.processScreening(s, true));
        verifyNoInteractions(dqs, model, subjects);
    }

    // The model service writes its result as its own user; sending that version again would hand it straight back.
    @Test
    public void lastChangedByTheHighRiskServiceIsSkipped() {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        encounters.touch(s, "High-Risk@tanuh_uat_local");

        assertEquals(ScreeningOutcome.SKIPPED, worker.processScreening(s, true));
        verifyNoInteractions(dqs, model, subjects);
    }

    @Test
    public void anOpenErrorRecordIsSkippedOnlyWhenAsked() {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        lenient().when(errorRecords.findByAvniEntityTypeAndEntityId(AvniEntityType.GeneralEncounter, "s1")).thenReturn(new ErrorRecord());
        dqsSays(passed());

        assertEquals(ScreeningOutcome.SKIPPED, worker.processScreening(s, true));
        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, false));
        assertEquals(2, encounters.patchBodies.size());
    }

    // A worker's edit synced while the job checked the copy it listed must not be overwritten with a stale verdict.
    @Test
    public void aScreeningChangedWhileBeingCheckedIsNeitherHandedOverNorWritten() {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        when(dqs.submit(eq(DqsClient.ORAL_SCREENING), any(Supplier.class))).thenAnswer(invocation -> {
            encounters.workerEdits("s1");
            return passed();
        });

        assertEquals(ScreeningOutcome.CHANGED, worker.processScreening(s, true));

        assertTrue(encounters.patchedUuids.isEmpty());
        verifyNoInteractions(model, errors);
    }

    @Test
    public void aDataQualityFailureWritesNothingAndWaitsForARetry() {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        when(dqs.submit(eq(DqsClient.ORAL_SCREENING), any(Supplier.class))).thenThrow(new DqsException("DQS cannot take oral_screening s1 right now (503): busy"));

        assertEquals(ScreeningOutcome.FAILED, worker.processScreening(s, true));

        assertTrue(encounters.patchBodies.isEmpty());
        verify(errors).errorOccurred("s1", "DataQualityCallFailed", "DqsException: DQS cannot take oral_screening s1 right now (503): busy");
        verifyNoInteractions(model);
    }

    // The rules that compare the screening with the patient read the registration, so it has to be there first.
    @Test
    public void thePatientsRegistrationIsSentBeforeTheScreening() {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        Subject patient = patient(false);
        patient.setUuid("patient-of-s1");
        patient.set("Registration date", "2026-07-03");
        when(subjects.getSubjectOrThrow("patient-of-s1")).thenReturn(patient);
        dqsSays(failedRule());

        worker.processScreening(s, true);

        ArgumentCaptor<Map<String, Object>> registration = ArgumentCaptor.forClass(Map.class);
        InOrder order = inOrder(dqs);
        order.verify(dqs).submit(eq(DqsClient.REGISTRATION), registration.capture());
        order.verify(dqs).submit(eq(DqsClient.ORAL_SCREENING), any(Supplier.class));
        assertEquals("patient-of-s1", registration.getValue().get("record_id"));
        assertEquals("2026-07-03", registration.getValue().get("registered_on"));
    }

    @Test
    public void aRegistrationTheServiceCannotTakeHoldsTheScreeningForARetry() {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        when(dqs.submit(eq(DqsClient.REGISTRATION), anyMap())).thenThrow(new DqsException("DQS cannot take registration p1 right now (503): busy"));

        assertEquals(ScreeningOutcome.FAILED, worker.processScreening(s, true));

        verify(dqs, never()).submit(eq(DqsClient.ORAL_SCREENING), any(Supplier.class));
        verify(errors).errorOccurred("s1", "DataQualityCallFailed", "DqsException: DQS cannot take registration p1 right now (503): busy");
        assertTrue(encounters.patchBodies.isEmpty());
        verifyNoInteractions(model);
    }

    // The verdict is written before the hand-over, so a hand-over that fails leaves it standing and only the hand-over
    // waits for the retry.
    @Test
    public void aModelHandOverFailureKeepsTheVerdictAndWaitsForTheHandOver() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        dqsSays(passed());
        doThrow(new HighRiskModelException("Model service answered 500 for case s1: busy")).when(model).submit(any(), anyList());

        assertEquals(ScreeningOutcome.FAILED, worker.processScreening(s, true));

        assertEquals(List.of(verdict("QC Pass")), encounters.patchBodies);
        verify(errors).errorOccurred("s1", "HighRiskModelCallFailed", "HighRiskModelException: Model service answered 500 for case s1: busy");
    }

    @Test
    public void theVerdictIsOnTheScreeningBeforeItIsHandedOver() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        dqsSays(passed());
        List<Integer> writtenAtHandOver = new ArrayList<>();
        doAnswer(invocation -> {
            writtenAtHandOver.add(encounters.patchBodies.size());
            return new Submission(Submission.Outcome.ACCEPTED, "{}");
        }).when(model).submit(anyString(), anyList());

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        assertEquals(List.of(1), writtenAtHandOver);
    }

    @Test
    public void handingOverAgainSendsOnlyAScreeningStillMarkedPass() throws Exception {
        GeneralEncounter marked = screeningWithPhotos("s1", "No");
        marked.addObservation("QC status", "QC Pass");
        encounters.touch(marked, JOB_USER);
        GeneralEncounter failed = screeningWithPhotos("s2", "No");
        failed.addObservation("QC status", "QC Fail");
        encounters.touch(failed, JOB_USER);

        assertEquals(ScreeningOutcome.WRITTEN, worker.handOverAgain(marked));
        assertEquals(ScreeningOutcome.SKIPPED, worker.handOverAgain(failed));

        verify(model).submit(eq("s1"), eq(List.of("s3://prod-user-media/tanuh_uat/s1-0.jpg")));
        verifyNoMoreInteractions(model);
        verifyNoInteractions(dqs);
        assertEquals(List.of("s1"), encounters.patchedUuids, "only Tanuh's answer is written, not a second verdict");
        assertEquals(List.of(callMade("Yes")), encounters.patchBodies);
    }

    @Test
    public void aScreeningAlreadySentIsCheckedButNotSentAgain() {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        s.addObservation("OCSHR Call Made", "Yes");
        dqsSays(passed());

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        assertEquals(List.of(verdict("QC Pass")), encounters.patchBodies);
        verifyNoInteractions(model);
    }

    @Test
    public void aSentScreeningThatNowFailsQcKeepsItsYes() {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        s.addObservation("OCSHR Call Made", "Yes");
        dqsSays(failedRule());

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        assertEquals(List.of(verdict("QC Fail")), encounters.patchBodies);
        verifyNoInteractions(model);
    }

    @Test
    public void aScreeningMarkedNotSentStaysNotSent() {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        s.addObservation("OCSHR Call Made", "No");
        dqsSays(passed());

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        assertEquals(List.of(verdict("QC Pass")), encounters.patchBodies);
        verifyNoInteractions(model);
    }

    @Test
    public void handingOverAgainSkipsAScreeningWhoseCallMadeIsSet() {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        s.addObservation("QC status", "QC Pass");
        s.addObservation("OCSHR Call Made", "Yes");

        assertEquals(ScreeningOutcome.SKIPPED, worker.handOverAgain(s));

        verifyNoInteractions(model, dqs);
        assertTrue(encounters.patchBodies.isEmpty());
    }

    @Test
    public void aRefusedPatientReadIsRecordedNotSkipped() {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        when(subjects.getSubjectOrThrow("patient-of-s1"))
                .thenThrow(HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden", HttpHeaders.EMPTY, null, null));

        assertEquals(ScreeningOutcome.FAILED, worker.processScreening(s, true));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(errors).errorOccurred(eq("s1"), eq("ScreeningProcessingFailed"), message.capture());
        assertTrue(message.getValue().contains("403"), message.getValue());
        assertTrue(encounters.patchBodies.isEmpty());
    }

    @Test
    public void anAcceptedScreeningIsMarkedSent() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No", "Yes");
        dqsSays(passed());

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        verify(model).submit("s1", List.of("s3://prod-user-media/tanuh_uat/s1-0.jpg", "s3://prod-user-media/tanuh_uat/s1-1.jpg"));
        assertEquals(List.of(verdict("QC Pass"), callMade("Yes")), encounters.patchBodies);
        assertEquals("Yes", encounters.screenings.get("s1").getObservation("OCSHR Call Made"));
    }

    // OCSHR Call Made says whether the call was made (Adam, 17 Nov 2026). Tanuh refused this one, so it will not be
    // scored, but the call was made: it is marked Yes, never sent again, and reported so a person can take it to Tanuh.
    @Test
    public void aScreeningTheServiceRefusesIsMarkedSentAndReported() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        dqsSays(passed());
        when(model.submit(anyString(), anyList()))
                .thenReturn(new Submission(Submission.Outcome.REFUSED, "{\"error\":\"INVALID_MANIFEST\",\"problems\":[\"image 1: not found\"]}"));

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        assertEquals(List.of(verdict("QC Pass"), callMade("Yes")), encounters.patchBodies);
        assertEquals(Optional.of(Concept.YES), recordedAnswer("s1"));
        verifyNoInteractions(errors);
        verify(alerts).screeningNeedsAPerson(eq("s1"), eq(TanuhAlerts.Reason.REFUSED), contains("INVALID_MANIFEST"));
    }

    // Tanuh's service already holds this case with other photos, which are the ones it scores. The call was made, so it
    // is marked Yes and reported, and nothing waits for a person in an error record.
    @Test
    public void aScreeningTheServiceHoldsWithOtherPhotosIsMarkedSentAndReported() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        dqsSays(passed());
        String detail = "{\"error\":\"CASE_EXISTS_WITH_DIFFERENT_MANIFEST\",\"case_id\":\"s1\"}";
        when(model.submit(anyString(), anyList())).thenReturn(new Submission(Submission.Outcome.CONFLICT, detail));

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        assertEquals(List.of(verdict("QC Pass"), callMade("Yes")), encounters.patchBodies);
        assertEquals(Optional.of(Concept.YES), recordedAnswer("s1"));
        verifyNoInteractions(errors);
        verify(alerts).screeningNeedsAPerson(eq("s1"), eq(TanuhAlerts.Reason.HELD_WITH_OTHER_PHOTOS), contains("CASE_EXISTS_WITH_DIFFERENT_MANIFEST"));
    }

    @Test
    public void aFailedHandOverWritesNothingAndWaits() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        dqsSays(passed());
        when(model.submit(anyString(), anyList())).thenThrow(new HighRiskModelException("HTTP 503"));

        assertEquals(ScreeningOutcome.FAILED, worker.processScreening(s, true));

        verify(errors).errorOccurred("s1", "HighRiskModelCallFailed", "HighRiskModelException: HTTP 503");
        assertEquals(List.of(verdict("QC Pass")), encounters.patchBodies);
        assertNull(encounters.screenings.get("s1").getObservation("OCSHR Call Made"));
    }

    // A worker's edit synced while Tanuh's service answered is checked on its next listing, not hidden by the job's write.
    @Test
    public void anEditDuringTheHandOverIsNotWrittenOver() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        dqsSays(passed());
        when(model.submit(anyString(), anyList())).thenAnswer(invocation -> {
            encounters.workerEdits("s1");
            return new Submission(Submission.Outcome.ACCEPTED, "{}");
        });

        assertEquals(ScreeningOutcome.CHANGED, worker.processScreening(s, true));

        assertEquals(List.of(verdict("QC Pass")), encounters.patchBodies);
        verifyNoInteractions(errors);
    }

    // Tanuh's service can write its result before the job writes Call Made, and that is no reason to leave it empty.
    @Test
    public void aResultThatLandsFirstStillGetsYes() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        dqsSays(passed());
        when(model.submit(anyString(), anyList())).thenAnswer(invocation -> {
            encounters.touch(encounters.screenings.get("s1"), "High-Risk@tanuh_uat_local");
            return new Submission(Submission.Outcome.ACCEPTED, "{}");
        });

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        assertEquals(List.of(verdict("QC Pass"), callMade("Yes")), encounters.patchBodies);
    }

    // The job stopped between Tanuh's acceptance and the Call Made write. Its verdict stands, so it is not checked again.
    @Test
    public void aHandOverCutShortIsFinishedOnTheNextListing() throws Exception {
        GeneralEncounter s = handOverCutShort("s1", JOB_USER);

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        verify(model).submit("s1", List.of("s3://prod-user-media/tanuh_uat/s1-0.jpg"));
        assertEquals(List.of(callMade("Yes")), encounters.patchBodies);
        verifyNoInteractions(dqs, subjects);
    }

    // A send that timed out although Tanuh stored it: Tanuh's result is the last change, and the resend gets its 200.
    @Test
    public void aScreeningTanuhAlreadyScoredIsMarkedSent() throws Exception {
        GeneralEncounter s = handOverCutShort("s1", HIGH_RISK_USER);

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        verify(model).submit("s1", List.of("s3://prod-user-media/tanuh_uat/s1-0.jpg"));
        assertEquals(List.of(callMade("Yes")), encounters.patchBodies);
        verifyNoInteractions(dqs, subjects);
    }

    @Test
    public void aPendingHandOverWaitsWhileItsRecordIsOpen() {
        GeneralEncounter s = handOverCutShort("s1", JOB_USER);
        when(errorRecords.findByAvniEntityTypeAndEntityId(AvniEntityType.GeneralEncounter, "s1")).thenReturn(new ErrorRecord());

        assertEquals(ScreeningOutcome.SKIPPED, worker.processScreening(s, true));

        verify(errorRecords).findByAvniEntityTypeAndEntityId(AvniEntityType.GeneralEncounter, "s1");
        verifyNoInteractions(model, dqs, subjects);
        assertTrue(encounters.patchBodies.isEmpty());
    }

    // The data quality service gets each photo as a link Avni signs. Avni answers 403 for a photo in another
    // organisation's folder or another bucket: that screening has a photo outside the organisation's media storage, so it
    // fails QC and is marked not sent in one write, without asking the service, and a person is told (Adam, 17 Nov 2026).
    @Test
    public void aPhotoAvniRefusesFailsQcAndIsMarkedNotSent() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No", "No");
        dqsBuildsTheRecord();
        when(downloader.sign(contains("s1-1"))).thenThrow(new PhotoOutsideStorageException("outside", null));

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        assertEquals(List.of(failVerdict()), encounters.patchBodies);
        verifyNoInteractions(model, errors);
        verify(alerts).screeningNeedsAPerson(eq("s1"), eq(TanuhAlerts.Reason.PHOTO_OUTSIDE_STORAGE), anyString());
    }

    @Test
    public void aSentScreeningWithAPhotoAvniRefusesKeepsItsYes() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        s.addObservation("OCSHR Call Made", "Yes");
        dqsBuildsTheRecord();
        when(downloader.sign(anyString())).thenThrow(new PhotoOutsideStorageException("outside", null));

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        assertEquals(List.of(verdict("QC Fail")), encounters.patchBodies);
        verifyNoInteractions(model, errors);
    }

    // A hand-over that skips the quality check (cut short, or retried) signs every photo before the send.
    @Test
    public void aPhotoAvniRefusesOnAHandOverIsMarkedNotSent() throws Exception {
        GeneralEncounter s = handOverCutShort("s1", JOB_USER);
        when(downloader.sign(anyString())).thenThrow(new PhotoOutsideStorageException("outside", null));

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        assertEquals(List.of(callMade("No")), encounters.patchBodies);
        verifyNoInteractions(model, errors);
        verify(alerts).screeningNeedsAPerson(eq("s1"), eq(TanuhAlerts.Reason.PHOTO_OUTSIDE_STORAGE), contains("photo 1"));
    }

    // Any other failure to sign is not a verdict on the photo: the screening waits and is sent again.
    @Test
    public void aPhotoAvniCannotSignForAnotherReasonWaits() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No", "No");
        dqsSays(passed());
        when(downloader.sign(contains("s1-1"))).thenThrow(HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", HttpHeaders.EMPTY, null, null));

        assertEquals(ScreeningOutcome.FAILED, worker.processScreening(s, true));

        verify(model, never()).submit(anyString(), anyList());
        verify(errors).errorOccurred(eq("s1"), eq("HighRiskModelCallFailed"), contains("could not sign photo 2"));
        assertEquals(List.of(verdict("QC Pass")), encounters.patchBodies);
        verifyNoInteractions(alerts);
    }

    // Tanuh's service takes 1 to 13 photos per case (its guide, section 8). A screening with more sends its first 13, in
    // the form's order, rather than earning a refusal and a No for good (Adam, 17 Nov 2026).
    @Test
    public void moreThanThirteenPhotosSendTheFirstThirteen() throws Exception {
        String[] fourteen = new String[14];
        java.util.Arrays.fill(fourteen, "No");
        GeneralEncounter s = screeningWithPhotos("s1", fourteen);
        dqsSays(passed());

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        List<String> firstThirteen = new ArrayList<>();
        for (int i = 0; i < 13; i++) firstThirteen.add("s3://prod-user-media/tanuh_uat/s1-" + i + ".jpg");
        verify(model).submit("s1", firstThirteen);
        assertEquals(List.of(verdict("QC Pass"), callMade("Yes")), encounters.patchBodies);
        verifyNoInteractions(errors);
    }

    @Test
    public void thirteenPhotosAreSent() throws Exception {
        String[] thirteen = new String[13];
        java.util.Arrays.fill(thirteen, "No");
        GeneralEncounter s = screeningWithPhotos("s1", thirteen);
        dqsSays(passed());

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        verify(model).submit(eq("s1"), argThat(uris -> uris.size() == 13));
    }

    // An answer the job does not know is a failed call: OCSHR Call Made stays empty and the same request goes again.
    @Test
    public void anAnswerTheJobDoesNotKnowIsAFailedCallThatLeavesCallMadeEmpty() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        dqsSays(passed());
        when(model.submit(anyString(), anyList())).thenReturn(new Submission(null, "{}"));

        assertEquals(ScreeningOutcome.FAILED, worker.processScreening(s, true));

        verify(errors).errorOccurred(eq("s1"), eq("HighRiskModelCallFailed"), anyString());
        assertEquals(List.of(verdict("QC Pass")), encounters.patchBodies);
        assertEquals(Optional.empty(), recordedAnswer("s1"));
    }

    // A worker's edit synced after the listing: the listed copy is not sent, and the newer version is read next.
    @Test
    public void aHandOverCutShortThatChangedSinceItWasListedIsNotSent() throws Exception {
        GeneralEncounter stored = handOverCutShort("s1", JOB_USER);
        GeneralEncounter listed = new GeneralEncounter();
        stored.getProperties().forEach(listed::set);
        encounters.workerEdits("s1");

        assertEquals(ScreeningOutcome.CHANGED, worker.processScreening(listed, true));

        verifyNoInteractions(model);
        assertTrue(encounters.patchBodies.isEmpty());
    }

    // A phone's edit replaces every answer on the screening with the phone's copy. A phone that had not received the
    // job's answers sends a copy without them, which looks like a screening never checked or sent.
    private GeneralEncounter editedOnAPhoneWithoutTheJobsAnswers(String uuid) {
        GeneralEncounter s = encounters.screenings.get(uuid);
        for (String answer : List.of("QC status", "QC check time", "OCSHR Call Made")) s.getObservations().remove(answer);
        encounters.workerEdits(uuid);
        return s;
    }

    private static Map<String, Object> withCallMade(Map<String, Object> verdict, String answer) {
        Map<String, Object> values = new HashMap<>(verdict);
        values.put("OCSHR Call Made", answer);
        return values;
    }

    // A screening that went to Tanuh's model once never goes again after an edit (Adam, 17 Nov 2026). Its answer is put
    // back from the job's own record.
    @Test
    public void aSentScreeningWhoseAnswerAPhoneClearedIsNotSentAgain() throws Exception {
        dqsSays(passed());
        worker.processScreening(screeningWithPhotos("s1", "No"), true);

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(editedOnAPhoneWithoutTheJobsAnswers("s1"), true));

        verify(model, times(1)).submit(anyString(), anyList());
        assertEquals(withCallMade(verdict("QC Pass"), "Yes"), encounters.patchBodies.get(encounters.patchBodies.size() - 1));
        assertEquals("Yes", encounters.screenings.get("s1").getObservation("OCSHR Call Made"));
    }

    @Test
    public void aScreeningMarkedNotSentWhoseAnswerAPhoneClearedStaysNotSent() throws Exception {
        dqsSays(passed());
        worker.processScreening(encounters.add("s1", Map.of("Able to Open Mouth?", "No")), true);
        GeneralEncounter edited = editedOnAPhoneWithoutTheJobsAnswers("s1");
        edited.addObservation(PHOTOS, List.of(row("https://s3.ap-south-1.amazonaws.com/prod-user-media/tanuh_uat/s1-0.jpg", "No")));

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(edited, true));

        verifyNoInteractions(model);
        assertEquals(notSent("QC Pass"), encounters.patchBodies.get(encounters.patchBodies.size() - 1));
    }

    @Test
    public void anEditThatFailsQcAfterASendKeepsTheScreeningMarkedSent() throws Exception {
        dqsSays(passed());
        worker.processScreening(screeningWithPhotos("s1", "No"), true);
        dqsSays(failedRule());

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(editedOnAPhoneWithoutTheJobsAnswers("s1"), true));

        verify(model, times(1)).submit(anyString(), anyList());
        assertEquals(withCallMade(verdict("QC Fail"), "Yes"), encounters.patchBodies.get(encounters.patchBodies.size() - 1));
    }

    // Tanuh may already hold a screening whose send went unanswered. The send is repeated as it was, so an edit's
    // new photos never reach the model, and Tanuh answers the repeat as the same case.
    @Test
    public void anEditAfterASendWhoseAnswerWasLostRepeatsTheFirstPhotos() throws Exception {
        dqsSays(passed());
        when(model.submit(anyString(), anyList())).thenThrow(new HighRiskModelException("Model service at ocshr did not answer: Read timed out"));
        assertEquals(ScreeningOutcome.FAILED, worker.processScreening(screeningWithPhotos("s1", "No", "No"), true));
        GeneralEncounter edited = editedOnAPhoneWithoutTheJobsAnswers("s1");
        edited.addObservation(PHOTOS, List.of(row("https://s3.ap-south-1.amazonaws.com/prod-user-media/tanuh_uat/s1-9.jpg", "No")));
        reset(model);
        when(model.submit(anyString(), anyList())).thenReturn(new Submission(Submission.Outcome.ACCEPTED, "{}"));

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(edited, false));

        verify(model).submit("s1", List.of("s3://prod-user-media/tanuh_uat/s1-0.jpg", "s3://prod-user-media/tanuh_uat/s1-1.jpg"));
        verifyNoMoreInteractions(model);
        assertEquals("Yes", encounters.screenings.get("s1").getObservation("OCSHR Call Made"));
    }

    @Test
    public void aSendWhoseAnswerWasLostIsSettledEvenWhenTheEditFailsQc() throws Exception {
        dqsSays(passed());
        when(model.submit(anyString(), anyList())).thenThrow(new HighRiskModelException("Model service at ocshr did not answer: Read timed out"));
        worker.processScreening(screeningWithPhotos("s1", "No"), true);
        encounters.workerEdits("s1");
        dqsSays(failedRule());
        reset(model);
        when(model.submit(anyString(), anyList())).thenReturn(new Submission(Submission.Outcome.ACCEPTED, "{}"));

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(encounters.screenings.get("s1"), false));

        verify(model).submit("s1", List.of("s3://prod-user-media/tanuh_uat/s1-0.jpg"));
        assertEquals(List.of(verdict("QC Pass"), verdict("QC Fail"), callMade("Yes")), encounters.patchBodies);
    }

    @Test
    public void handingOverAgainSettlesASendWhoseAnswerWasLostWhateverTheVerdict() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        s.addObservation("QC status", "QC Fail");
        encounters.touch(s, JOB_USER);
        sent.attempt("s1", List.of("s3://prod-user-media/tanuh_uat/s1-0.jpg"));

        assertEquals(ScreeningOutcome.WRITTEN, worker.handOverAgain(s));

        verify(model).submit("s1", List.of("s3://prod-user-media/tanuh_uat/s1-0.jpg"));
        assertEquals(List.of(callMade("Yes")), encounters.patchBodies);
    }

    // Tanuh took the screening, but a worker's edit landed before Yes was written. The edit is checked on the next run,
    // which writes Yes from the job's record and sends nothing.
    @Test
    public void anEditDuringTheHandOverGetsItsYesOnTheNextRunWithoutASecondSend() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        dqsSays(passed());
        when(model.submit(anyString(), anyList())).thenAnswer(invocation -> {
            encounters.workerEdits("s1");
            return new Submission(Submission.Outcome.ACCEPTED, "{}");
        });
        assertEquals(ScreeningOutcome.CHANGED, worker.processScreening(s, true));

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(encounters.screenings.get("s1"), true));

        verify(model, times(1)).submit(anyString(), anyList());
        assertEquals(withCallMade(verdict("QC Pass"), "Yes"), encounters.patchBodies.get(encounters.patchBodies.size() - 1));
    }

    private Optional<Concept> recordedAnswer(String uuid) {
        return sent.find(uuid).flatMap(SentScreenings.SentScreening::callMade);
    }

    // A refusal is reported when the job records its Yes, even if a worker's edit keeps the Yes off the screening for now.
    @Test
    public void aRefusalIsReportedEvenWhenItsYesIsNotWrittenAtOnce() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        dqsSays(passed());
        when(model.submit(anyString(), anyList())).thenAnswer(invocation -> {
            encounters.workerEdits("s1");
            return new Submission(Submission.Outcome.REFUSED, "{\"error\":\"INVALID_MANIFEST\"}");
        });

        assertEquals(ScreeningOutcome.CHANGED, worker.processScreening(s, true));

        assertEquals(Optional.of(Concept.YES), recordedAnswer("s1"));
        verify(alerts).screeningNeedsAPerson(eq("s1"), eq(TanuhAlerts.Reason.REFUSED), contains("INVALID_MANIFEST"));
    }

    @Test
    public void aPhotoAvniRefusesOnAHandOverIsReportedEvenWhenItsNoIsNotWrittenAtOnce() throws Exception {
        GeneralEncounter s = handOverCutShort("s1", JOB_USER);
        when(downloader.sign(anyString())).thenAnswer(invocation -> {
            encounters.workerEdits("s1");
            throw new PhotoOutsideStorageException("outside", null);
        });

        assertEquals(ScreeningOutcome.CHANGED, worker.processScreening(s, true));

        assertEquals(Optional.of(Concept.NO), recordedAnswer("s1"));
        verify(alerts).screeningNeedsAPerson(eq("s1"), eq(TanuhAlerts.Reason.PHOTO_OUTSIDE_STORAGE), anyString());
    }

    // The answer is kept before it is written on the screening: a write that fails leaves it in the job's record, and
    // the retry writes it without a second send.
    @Test
    public void aYesWhoseWriteFailsIsKeptAndWrittenByTheRetryWithoutASecondSend() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        dqsSays(passed());
        when(model.submit(anyString(), anyList())).thenAnswer(invocation -> {
            encounters.failNextPatchWith = new RuntimeException("connection reset");
            return new Submission(Submission.Outcome.ACCEPTED, "{}");
        });
        assertEquals(ScreeningOutcome.FAILED, worker.processScreening(s, true));
        assertEquals(Optional.of(Concept.YES), recordedAnswer("s1"));
        assertNull(encounters.screenings.get("s1").getObservation("OCSHR Call Made"));
        reset(model);

        assertEquals(ScreeningOutcome.WRITTEN, worker.handOverAgain(encounters.screenings.get("s1")));

        verifyNoInteractions(model);
        assertEquals("Yes", encounters.screenings.get("s1").getObservation("OCSHR Call Made"));
    }

    // A send that failed waits for its retry with no answer in the job's record. A person who settles it with Tanuh
    // writes Yes on the screening (runbook, section 8); that Yes settles the record, so the screening is not sent again.
    @Test
    public void aPersonsAnswerOnAScreeningWhoseSendFailedSettlesItsRecordWithoutASend() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        dqsSays(passed());
        when(model.submit(anyString(), anyList())).thenThrow(new HighRiskModelException("Model service at tanuh did not answer: Read timed out"));
        assertEquals(ScreeningOutcome.FAILED, worker.processScreening(s, true));
        assertEquals(Optional.empty(), recordedAnswer("s1"));
        GeneralEncounter settled = encounters.screenings.get("s1");
        settled.addObservation("OCSHR Call Made", "Yes");
        encounters.workerEdits("s1");
        reset(model);

        // The retry job's path: the screening's error record is open.
        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(settled, false));

        verifyNoInteractions(model);
        assertEquals(Optional.of(Concept.YES), recordedAnswer("s1"));
    }

    // Tanuh's service writes its result, probability and version itself.
    @Test
    public void theJobNeverWritesTheModelsResult() throws Exception {
        dqsSays(passed());
        when(model.submit(eq("refused"), anyList())).thenReturn(new Submission(Submission.Outcome.REFUSED, "{\"error\":\"INVALID_MANIFEST\"}"));

        worker.processScreening(screeningWithPhotos("accepted", "No"), true);
        worker.processScreening(screeningWithPhotos("refused", "No"), true);
        worker.processScreening(encounters.add("bare", Map.of("Able to Open Mouth?", "No")), true);

        assertEquals(List.of("accepted", "accepted", "refused", "refused", "bare"), encounters.patchedUuids);
        Set<String> written = encounters.patchBodies.stream().flatMap(body -> body.keySet().stream()).collect(Collectors.toSet());
        assertTrue(Set.of("QC status", "QC check time", "OCSHR Call Made").containsAll(written), written.toString());
    }
}
