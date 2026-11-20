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
import org.avni_integration_service.integration_data.domain.error.ErrorType;
import org.avni_integration_service.integration_data.domain.error.ErrorTypeFollowUpStep;
import org.avni_integration_service.integration_data.repository.ErrorRecordRepository;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.avni_integration_service.tanuh.config.TanuhConfig;
import org.avni_integration_service.tanuh.config.TanuhContextProvider;
import org.avni_integration_service.tanuh.dqs.DqsClient;
import org.avni_integration_service.tanuh.dqs.DqsException;
import org.avni_integration_service.tanuh.model.HighRiskModelClient;
import org.avni_integration_service.tanuh.model.HighRiskModelException;
import org.avni_integration_service.tanuh.model.Submission;
import org.avni_integration_service.tanuh.service.TanuhAlerts;
import org.avni_integration_service.tanuh.service.TanuhErrorService;
import org.avni_integration_service.tanuh.service.TanuhPhotoDownloader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TanuhScreeningRetryWorkerTest {
    static final String JOB_USER = "integration@tanuh_uat_local";
    static final String PHOTOS = "Take photos of all lesions and 1 photo without lesion";
    static final String CONFLICT = "{\"error\":\"CASE_EXISTS_WITH_DIFFERENT_MANIFEST\",\"case_id\":\"s1\"}";

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
    private TanuhScreeningRetryWorker retryWorker;

    @BeforeEach
    public void setUp() throws Exception {
        // The patient's registration goes ahead of every screening; the service stores it and the tests leave it be.
        lenient().when(dqs.submit(eq(DqsClient.REGISTRATION), anyMap())).thenReturn(ProcessScreeningTest.passed());
        IntegrationSystemConfig user = new IntegrationSystemConfig();
        user.setKey("avni_user");
        user.setValue(JOB_USER);
        IntegrationSystemConfig highRiskUser = new IntegrationSystemConfig();
        highRiskUser.setKey("high_risk_avni_user");
        highRiskUser.setValue(ProcessScreeningTest.HIGH_RISK_USER);
        IntegrationSystem system = new IntegrationSystem();
        system.setId(15);
        ReflectionTestUtils.setField(system, "name", "tanuh_uat_local");
        context.set(new TanuhConfig(new IntegrationSystemConfigCollection(List.of(user, highRiskUser)), system));
        context.setConcepts(TanuhConceptsFixture.defaults());
        encounters = new FakeAvniEncounterRepository(JOB_USER, Date.from(Instant.parse("2026-10-07T00:00:00Z")));
        OralScreeningWorker worker = new OralScreeningWorker(encounters, subjects, dqs, downloader, model, errors, alerts, new FakeSentScreenings(), errorRecords,
                statuses, context, ProcessScreeningTest.CLOCK, 10);
        retryWorker = new TanuhScreeningRetryWorker(errorRecords, encounters, worker, errors, context);
        lenient().when(subjects.getSubjectOrThrow(anyString())).thenReturn(patient(false));
        // Tanuh's model service takes every screening unless a test says otherwise.
        lenient().when(model.submit(anyString(), anyList(), any())).thenReturn(new Submission(Submission.Outcome.ACCEPTED, "{}"));
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

    private static List<Map<String, Object>> photoRows(String uuid, String suspicious) {
        Map<String, Object> row = new HashMap<>();
        row.put("Oral Image", "https://s3.ap-south-1.amazonaws.com/prod-user-media/tanuh_uat/" + uuid + "-0.jpg");
        row.put("Suspicious Lesion?", suspicious);
        return List.of(row);
    }

    private GeneralEncounter screening(String uuid, String suspicious) {
        return encounters.add(uuid, Map.of(PHOTOS, photoRows(uuid, suspicious)));
    }

    private static ErrorRecord waiting(String uuid, ErrorTypeFollowUpStep followUpStep) {
        return waiting(uuid, followUpStep, TanuhErrorService.MODEL_CALL_FAILED);
    }

    private static ErrorRecord waiting(String uuid, ErrorTypeFollowUpStep followUpStep, String errorTypeName) {
        ErrorType type = new ErrorType();
        type.setName(errorTypeName);
        type.setFollowUpStep(followUpStep);
        ErrorRecord record = new ErrorRecord();
        record.setAvniEntityType(AvniEntityType.GeneralEncounter);
        record.setEntityId(uuid);
        record.addErrorLog(type, "HighRiskModelException: model down", null);
        return record;
    }

    private void waitingAre(ErrorRecord... records) {
        when(errorRecords.getProcessableErrorRecords()).thenReturn(List.of(records));
    }

    private void dqsPasses() {
        when(dqs.submit(eq(DqsClient.ORAL_SCREENING), any(Supplier.class))).thenReturn(ProcessScreeningTest.passed());
    }

    @Test
    public void aDeletedScreeningStopsWaitingWithoutASend() {
        screening("s1", "No").setVoided(true);
        ErrorRecord record = waiting("s1", ErrorTypeFollowUpStep.Process);
        waitingAre(record);

        assertTrue(retryWorker.processWaiting());

        verify(errorRecords).delete(record);
        verifyNoInteractions(dqs, model);
        assertTrue(encounters.patchedUuids.isEmpty());
    }

    @Test
    public void aScreeningTheJobAlreadyWroteStopsWaitingWithoutASend() {
        encounters.touch(screening("s1", "No"), JOB_USER);
        ErrorRecord record = waiting("s1", ErrorTypeFollowUpStep.Process, TanuhErrorService.DATA_QUALITY_CALL_FAILED);
        waitingAre(record);

        assertTrue(retryWorker.processWaiting());

        verify(errorRecords).delete(record);
        verifyNoInteractions(dqs, model);
    }

    @Test
    public void aScreeningNoLongerOnTheServerStopsWaiting() {
        ErrorRecord record = waiting("gone", ErrorTypeFollowUpStep.Process);
        waitingAre(record);

        assertTrue(retryWorker.processWaiting());

        verify(errorRecords).delete(record);
        verifyNoInteractions(dqs, model);
    }

    @Test
    public void aCheckedRetryWritesTheVerdictAndStopsWaiting() throws Exception {
        screening("s1", "No");
        dqsPasses();
        ErrorRecord record = waiting("s1", ErrorTypeFollowUpStep.Process);
        waitingAre(record);

        assertTrue(retryWorker.processWaiting());

        assertEquals(List.of("s1", "s1"), encounters.patchedUuids, "the verdict, then Tanuh's answer");
        assertEquals("QC Pass", encounters.screenings.get("s1").getObservation("QC status"));
        verify(model).submit(any(), anyList(), any());
        verify(errorRecords).delete(record);
    }

    @Test
    public void aFailedRetryKeepsTheRecordWithAFreshLogAndReportsFailure() throws Exception {
        screening("s1", "No");
        dqsPasses();
        doThrow(new HighRiskModelException("model down")).when(model).submit(any(), anyList(), any());
        waitingAre(waiting("s1", ErrorTypeFollowUpStep.Process));

        assertFalse(retryWorker.processWaiting());

        verify(errors).errorOccurred("s1", TanuhErrorService.MODEL_CALL_FAILED, "HighRiskModelException: model down");
        verify(errorRecords, never()).delete(any(ErrorRecord.class));
        assertEquals(List.of("s1"), encounters.patchedUuids, "the verdict stands; only the hand-over waits");
    }

    // The verdict is on the screening and only the hand-over failed, so the retry hands it over without a second check.
    @Test
    public void aWrittenScreeningWhoseHandOverFailedIsHandedOverAgainWithoutASecondCheck() throws Exception {
        GeneralEncounter s = screening("s1", "No");
        s.addObservation("QC status", "QC Pass");
        encounters.touch(s, JOB_USER);
        ErrorRecord record = waiting("s1", ErrorTypeFollowUpStep.Process);
        waitingAre(record);

        assertTrue(retryWorker.processWaiting());

        verify(model).submit(eq("s1"), eq(List.of("s3://prod-user-media/tanuh_uat/s1-0.jpg")), any());
        verifyNoInteractions(dqs);
        assertEquals(List.of("s1"), encounters.patchedUuids, "only Tanuh's answer is written, not a second verdict");
        assertEquals(List.of(ProcessScreeningTest.callMade("Yes")), encounters.patchBodies);
        verify(errorRecords).delete(record);
    }

    // A failed send opened the record, then a retry could not read the screening, so the record's newest reason is no
    // longer the model call. The screening still waits to be sent: the main job skipped it while the record was open,
    // and its read position has moved past, so this retry is the only way it is ever sent.
    @Test
    public void aWaitingHandOverWhoseRecordLastFailedOnAReadIsStillSent() throws Exception {
        GeneralEncounter s = screening("s1", "No");
        s.addObservation("QC status", "QC Pass");
        encounters.touch(s, JOB_USER);
        ErrorRecord record = waiting("s1", ErrorTypeFollowUpStep.Process, TanuhErrorService.SCREENING_PROCESSING_FAILED);
        waitingAre(record);

        assertTrue(retryWorker.processWaiting());

        verify(model).submit(eq("s1"), eq(List.of("s3://prod-user-media/tanuh_uat/s1-0.jpg")), any());
        assertEquals(List.of(ProcessScreeningTest.callMade("Yes")), encounters.patchBodies);
        verify(errorRecords).delete(record);
    }

    // The same record on a screening that is no longer waiting, such as one marked QC Fail and No by a verdict write
    // whose reply was lost: it is settled without a send.
    @Test
    public void aRecordOnAScreeningNoLongerWaitingIsSettledWithoutASend() throws Exception {
        GeneralEncounter s = screening("s1", "No");
        s.addObservation("QC status", "QC Fail");
        s.addObservation("OCSHR Call Made", "No");
        encounters.touch(s, JOB_USER);
        ErrorRecord record = waiting("s1", ErrorTypeFollowUpStep.Process, TanuhErrorService.SCREENING_PROCESSING_FAILED);
        waitingAre(record);

        assertTrue(retryWorker.processWaiting());

        verifyNoInteractions(model);
        assertTrue(encounters.patchBodies.isEmpty());
        verify(errorRecords).delete(record);
    }

    @Test
    public void aHandOverThatStillFailsKeepsWaiting() throws Exception {
        GeneralEncounter s = screening("s1", "No");
        s.addObservation("QC status", "QC Pass");
        encounters.touch(s, JOB_USER);
        doThrow(new HighRiskModelException("model down")).when(model).submit(any(), anyList(), any());
        waitingAre(waiting("s1", ErrorTypeFollowUpStep.Process));

        assertFalse(retryWorker.processWaiting());

        verify(errors).errorOccurred("s1", TanuhErrorService.MODEL_CALL_FAILED, "HighRiskModelException: model down");
        verify(errorRecords, never()).delete(any(ErrorRecord.class));
        verifyNoInteractions(dqs);
    }

    @Test
    public void aWrittenScreeningNoLongerMarkedPassIsNotHandedOver() {
        GeneralEncounter s = screening("s1", "No");
        s.addObservation("QC status", "QC Fail");
        encounters.touch(s, JOB_USER);
        ErrorRecord record = waiting("s1", ErrorTypeFollowUpStep.Process);
        waitingAre(record);

        assertTrue(retryWorker.processWaiting());

        verifyNoInteractions(dqs, model);
        verify(errorRecords).delete(record);
    }

    // The record holds only the screening's id, so the retry sends the screening as it is now: a worker's edit
    // made while it waited is what gets checked.
    @Test
    public void theRetrySendsTheScreeningAsItIsNow() {
        screening("s1", "No");
        encounters.screenings.get("s1").addObservation("Any Symptoms", "Yes");
        encounters.workerEdits("s1");
        dqsPasses();
        waitingAre(waiting("s1", ErrorTypeFollowUpStep.Process));

        assertTrue(retryWorker.processWaiting());

        ArgumentCaptor<Supplier<Map<String, Object>>> record = ArgumentCaptor.forClass(Supplier.class);
        verify(dqs).submit(eq(DqsClient.ORAL_SCREENING), record.capture());
        assertEquals(true, record.getValue().get().get("any_symptoms"));
    }

    @Test
    public void aDataQualityFailureKeepsTheScreeningWaiting() {
        screening("s1", "No");
        when(dqs.submit(eq(DqsClient.ORAL_SCREENING), any(Supplier.class))).thenThrow(new DqsException("DQS at dqs did not answer"));
        waitingAre(waiting("s1", ErrorTypeFollowUpStep.Process));

        assertFalse(retryWorker.processWaiting());

        verify(errors).errorOccurred("s1", TanuhErrorService.DATA_QUALITY_CALL_FAILED, "DqsException: DQS at dqs did not answer");
        verify(errorRecords, never()).delete(any(ErrorRecord.class));
    }

    @Test
    public void aScreeningChangedDuringItsRetryKeepsWaitingWithoutFailing() {
        screening("s1", "No");
        when(dqs.submit(eq(DqsClient.ORAL_SCREENING), any(Supplier.class))).thenAnswer(invocation -> {
            encounters.workerEdits("s1");
            return ProcessScreeningTest.passed();
        });
        waitingAre(waiting("s1", ErrorTypeFollowUpStep.Process));

        assertTrue(retryWorker.processWaiting());

        verify(errorRecords, never()).delete(any(ErrorRecord.class));
        assertTrue(encounters.patchedUuids.isEmpty());
    }

    @Test
    public void aPatientDeletedWhileTheScreeningWaitedStopsItWaiting() {
        screening("s1", "No");
        when(subjects.getSubjectOrThrow(anyString())).thenReturn(patient(true));
        ErrorRecord record = waiting("s1", ErrorTypeFollowUpStep.Process);
        waitingAre(record);

        assertTrue(retryWorker.processWaiting());

        verify(errorRecords).delete(record);
        verifyNoInteractions(dqs, model);
    }

    @Test
    public void aRecordWhoseLastErrorIsTerminalIsLeftAlone() {
        screening("s1", "No");
        waitingAre(waiting("s1", ErrorTypeFollowUpStep.Terminal));

        assertTrue(retryWorker.processWaiting());

        verify(errorRecords, never()).delete(any(ErrorRecord.class));
        verifyNoInteractions(dqs, model);
    }

    @Test
    public void oneFailedRetryReportsFailureAndTheOthersAreStillTried() throws Exception {
        screening("s1", "No");
        screening("s2", "No");
        dqsPasses();
        doAnswer(invocation -> {
            if ("s1".equals(invocation.getArgument(0))) throw new HighRiskModelException("model down");
            return new Submission(Submission.Outcome.ACCEPTED, "{}");
        }).when(model).submit(anyString(), anyList(), any());
        ErrorRecord first = waiting("s1", ErrorTypeFollowUpStep.Process);
        ErrorRecord second = waiting("s2", ErrorTypeFollowUpStep.Process);
        waitingAre(first, second);

        assertFalse(retryWorker.processWaiting());

        verify(errorRecords, never()).delete(first);
        verify(errorRecords).delete(second);
    }

    @Test
    public void aScreeningThatCannotBeReadKeepsWaitingAndReportsFailure() {
        waitingAre(waiting("s1", ErrorTypeFollowUpStep.Process));
        encounters.failReadsWith = HttpServerErrorException.create(HttpStatus.BAD_GATEWAY, "Bad Gateway", HttpHeaders.EMPTY, null, null);

        assertFalse(retryWorker.processWaiting());

        verify(errors).errorOccurred(eq("s1"), eq(TanuhErrorService.SCREENING_PROCESSING_FAILED), startsWith("BadGateway"));
        verify(errorRecords, never()).delete(any(ErrorRecord.class));
    }

    private static HttpClientErrorException notFound() {
        return HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, null, null);
    }

    // Review finding: sign-in happens on a run's first call, and a 404 there (a wrong address, a proxy) read as
    // "screening gone", so every waiting record was dropped behind a green check.
    @Test
    public void aWrongAddressOrFailedSignInFailsTheRunBeforeAnyRecordIsTouched() {
        screening("s1", "No");
        waitingAre(waiting("s1", ErrorTypeFollowUpStep.Process), waiting("gone", ErrorTypeFollowUpStep.Process));
        encounters.failListsWith = notFound();
        encounters.failReadsWith = notFound();

        assertThrows(HttpClientErrorException.NotFound.class, () -> retryWorker.processWaiting());

        verify(errorRecords, never()).delete(any(ErrorRecord.class));
        verifyNoInteractions(errors, dqs, model);
    }

    @Test
    public void processErrorAlsoFailsBeforeTouchingTheRecordWhenTheAddressOrSignInFails() {
        when(errorRecords.findByAvniEntityTypeAndEntityId(AvniEntityType.GeneralEncounter, "s1")).thenReturn(waiting("s1", ErrorTypeFollowUpStep.Process));
        encounters.failListsWith = notFound();
        encounters.failReadsWith = notFound();

        assertThrows(HttpClientErrorException.NotFound.class, () -> retryWorker.processError("s1"));

        verify(errorRecords, never()).delete(any(ErrorRecord.class));
    }

    // A run with nothing waiting makes no call to Avni, as before.
    @Test
    public void nothingWaitingMakesNoAvniCall() {
        waitingAre();

        assertTrue(retryWorker.processWaiting());

        assertEquals(0, encounters.listCalls);
    }

    @Test
    public void processErrorRetriesThatScreeningAndFailsLoudlyWhileItStillWaits() throws Exception {
        screening("s1", "No");
        dqsPasses();
        doThrow(new HighRiskModelException("model down")).when(model).submit(any(), anyList(), any());
        when(errorRecords.findByAvniEntityTypeAndEntityId(AvniEntityType.GeneralEncounter, "s1")).thenReturn(waiting("s1", ErrorTypeFollowUpStep.Process));

        assertThrows(IllegalStateException.class, () -> retryWorker.processError("s1"));

        verify(errorRecords, never()).delete(any(ErrorRecord.class));
    }

    @Test
    public void processErrorSettlesThatScreeningWhenItsRetrySucceeds() throws Exception {
        screening("s1", "No");
        dqsPasses();
        ErrorRecord record = waiting("s1", ErrorTypeFollowUpStep.Process);
        when(errorRecords.findByAvniEntityTypeAndEntityId(AvniEntityType.GeneralEncounter, "s1")).thenReturn(record);

        retryWorker.processError("s1");

        verify(errorRecords).delete(record);
    }

    // Tanuh holds the case with other photos. The call was made, so the retry marks it Yes, reports it and settles its
    // record: nothing waits for a person in the integration's error records.
    @Test
    public void aHandOverRetryThatMeetsAConflictIsMarkedSentAndSettled() throws Exception {
        GeneralEncounter s = screening("s1", "No");
        s.addObservation("QC status", "QC Pass");
        encounters.touch(s, JOB_USER);
        when(model.submit(anyString(), anyList(), any())).thenReturn(new Submission(Submission.Outcome.CONFLICT, CONFLICT));
        ErrorRecord record = waiting("s1", ErrorTypeFollowUpStep.Process);
        waitingAre(record);

        assertTrue(retryWorker.processWaiting());

        verify(errorRecords).delete(record);
        verifyNoInteractions(errors);
        verify(alerts).screeningNeedsAPerson(eq("s1"), eq(TanuhAlerts.Reason.HELD_WITH_OTHER_PHOTOS), contains("CASE_EXISTS_WITH_DIFFERENT_MANIFEST"));
        verifyNoInteractions(dqs);
        assertEquals("Yes", encounters.screenings.get("s1").getObservation("OCSHR Call Made"));
    }

    // The same conflict met by a retry that checks the screening in full.
    @Test
    public void aCheckedRetryThatMeetsAConflictIsMarkedSentAndSettled() throws Exception {
        screening("s1", "No");
        dqsPasses();
        when(model.submit(anyString(), anyList(), any())).thenReturn(new Submission(Submission.Outcome.CONFLICT, CONFLICT));
        ErrorRecord record = waiting("s1", ErrorTypeFollowUpStep.Process, TanuhErrorService.DATA_QUALITY_CALL_FAILED);
        waitingAre(record);

        assertTrue(retryWorker.processWaiting());

        verify(errorRecords).delete(record);
        verifyNoInteractions(errors);
        verify(alerts).screeningNeedsAPerson(eq("s1"), eq(TanuhAlerts.Reason.HELD_WITH_OTHER_PHOTOS), contains("CASE_EXISTS_WITH_DIFFERENT_MANIFEST"));
        assertEquals(List.of("s1", "s1"), encounters.patchedUuids, "the verdict, then OCSHR Call Made");
        assertEquals("Yes", encounters.screenings.get("s1").getObservation("OCSHR Call Made"));
    }

    // The send timed out although Tanuh stored the case and wrote its result. The screening, last changed by Tanuh's
    // service, is handed over again without a second check; Tanuh answers the identical resend with its 200.
    @Test
    public void aTimedOutSendThatTanuhScoredIsMarkedSent() throws Exception {
        GeneralEncounter s = screening("s1", "No");
        s.addObservation("QC status", "QC Pass");
        encounters.touch(s, ProcessScreeningTest.HIGH_RISK_USER);
        ErrorRecord record = waiting("s1", ErrorTypeFollowUpStep.Process);
        waitingAre(record);

        assertTrue(retryWorker.processWaiting());

        verify(model).submit(eq("s1"), eq(List.of("s3://prod-user-media/tanuh_uat/s1-0.jpg")), any());
        assertEquals(List.of(ProcessScreeningTest.callMade("Yes")), encounters.patchBodies);
        verify(errorRecords).delete(record);
        verifyNoInteractions(dqs);
    }
}
