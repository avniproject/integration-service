package org.avni_integration_service.tanuh.worker;

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
import org.avni_integration_service.tanuh.domain.ModelResult;
import org.avni_integration_service.tanuh.model.HighRiskModelClient;
import org.avni_integration_service.tanuh.model.HighRiskModelException;
import org.avni_integration_service.tanuh.service.RoutingPolicy;
import org.avni_integration_service.tanuh.service.Sampler;
import org.avni_integration_service.tanuh.service.TanuhErrorService;
import org.avni_integration_service.tanuh.service.TanuhPhotoDownloader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TanuhScreeningRetryWorkerTest {
    static final String JOB_USER = "integration@tanuh_uat_local";
    static final Instant RUN_TIME = Instant.parse("2026-10-07T09:15:00.123Z");
    static final String PHOTOS = "Take photos of all lesions and 1 photo without lesion";

    @Mock
    private AvniSubjectRepository subjects;
    @Mock
    private TanuhPhotoDownloader downloader;
    @Mock
    private HighRiskModelClient model;
    @Mock
    private Sampler sampler;
    @Mock
    private TanuhErrorService errors;
    @Mock
    private ErrorRecordRepository errorRecords;
    @Mock
    private IntegratingEntityStatusRepository statuses;
    @TempDir
    File photoDir;
    private final TanuhContextProvider context = new TanuhContextProvider();
    private FakeAvniEncounterRepository encounters;
    private TanuhScreeningRetryWorker retryWorker;

    @BeforeEach
    public void setUp() throws Exception {
        IntegrationSystemConfig user = new IntegrationSystemConfig();
        user.setKey("avni_user");
        user.setValue(JOB_USER);
        IntegrationSystem system = new IntegrationSystem();
        system.setId(15);
        ReflectionTestUtils.setField(system, "name", "tanuh_uat_local");
        context.set(new TanuhConfig(new IntegrationSystemConfigCollection(List.of(user)), system));
        encounters = new FakeAvniEncounterRepository(JOB_USER, Date.from(Instant.parse("2026-10-07T00:00:00Z")));
        OralScreeningWorker worker = new OralScreeningWorker(encounters, subjects, downloader, model, new RoutingPolicy(), sampler,
                errors, errorRecords, statuses, context, 10);
        retryWorker = new TanuhScreeningRetryWorker(errorRecords, encounters, worker, errors, context);
        lenient().when(subjects.getSubjectOrThrow(anyString())).thenReturn(patient(false));
        lenient().when(downloader.download(anyString())).thenAnswer(invocation -> Files.createTempFile(photoDir.toPath(), "photo", null).toFile());
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
        row.put("Oral Image", "minio://avni-user-media/t/" + uuid + ".jpg");
        row.put("Suspicious Lesion?", suspicious);
        return List.of(row);
    }

    private GeneralEncounter screening(String uuid, String suspicious) {
        return encounters.add(uuid, Map.of(PHOTOS, photoRows(uuid, suspicious)));
    }

    private static ErrorRecord waiting(String uuid, ErrorTypeFollowUpStep followUpStep) {
        ErrorType type = new ErrorType();
        type.setName(TanuhErrorService.MODEL_CALL_FAILED);
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

    private void modelSays(ModelResult.Result result) throws Exception {
        when(model.score(any(), anyList())).thenReturn(new ModelResult(result, "stub", RUN_TIME));
    }

    @Test
    public void aDeletedScreeningStopsWaitingWithoutAModelCall() {
        screening("s1", "No").setVoided(true);
        ErrorRecord record = waiting("s1", ErrorTypeFollowUpStep.Process);
        waitingAre(record);

        assertTrue(retryWorker.processWaiting());

        verify(errorRecords).delete(record);
        verifyNoInteractions(model);
        assertTrue(encounters.patchedUuids.isEmpty());
    }

    @Test
    public void aScreeningTheJobAlreadyWroteStopsWaitingWithoutAModelCall() {
        encounters.touch(screening("s1", "No"), JOB_USER);
        ErrorRecord record = waiting("s1", ErrorTypeFollowUpStep.Process);
        waitingAre(record);

        assertTrue(retryWorker.processWaiting());

        verify(errorRecords).delete(record);
        verifyNoInteractions(model);
    }

    @Test
    public void aScreeningNoLongerOnTheServerStopsWaiting() {
        ErrorRecord record = waiting("gone", ErrorTypeFollowUpStep.Process);
        waitingAre(record);

        assertTrue(retryWorker.processWaiting());

        verify(errorRecords).delete(record);
        verifyNoInteractions(model);
    }

    @Test
    public void aScoredRetryWritesTheFiveValuesAndStopsWaiting() throws Exception {
        screening("s1", "No");
        modelSays(ModelResult.Result.HIGH_RISK);
        ErrorRecord record = waiting("s1", ErrorTypeFollowUpStep.Process);
        waitingAre(record);

        assertTrue(retryWorker.processWaiting());

        assertEquals(List.of("s1"), encounters.patchedUuids);
        assertEquals("High Risk", encounters.screenings.get("s1").getObservation("Review category"));
        verify(errorRecords).delete(record);
    }

    @Test
    public void aFailedRetryKeepsTheRecordWithAFreshLogAndReportsFailure() throws Exception {
        screening("s1", "No");
        when(model.score(any(), anyList())).thenThrow(new HighRiskModelException("model down"));
        waitingAre(waiting("s1", ErrorTypeFollowUpStep.Process));

        assertFalse(retryWorker.processWaiting());

        verify(errors).errorOccurred("s1", TanuhErrorService.MODEL_CALL_FAILED, "HighRiskModelException: model down");
        verify(errorRecords, never()).delete(any(ErrorRecord.class));
        assertTrue(encounters.patchedUuids.isEmpty());
    }

    // The record holds only the screening's id, so the retry scores the screening as it is now: a worker's edit
    // made while it waited is what gets scored.
    @Test
    public void theRetryScoresTheScreeningAsItIsNow() throws Exception {
        screening("s1", "No");
        encounters.screenings.get("s1").addObservation(PHOTOS, photoRows("s1", "Yes"));
        encounters.workerEdits("s1");
        modelSays(ModelResult.Result.NOT_SUSPICIOUS);
        waitingAre(waiting("s1", ErrorTypeFollowUpStep.Process));

        assertTrue(retryWorker.processWaiting());

        assertEquals("FLW override", encounters.screenings.get("s1").getObservation("Review category"));
    }

    @Test
    public void aScreeningChangedDuringItsRetryKeepsWaitingWithoutFailing() throws Exception {
        screening("s1", "No");
        when(model.score(any(), anyList())).thenAnswer(invocation -> {
            encounters.workerEdits("s1");
            return new ModelResult(ModelResult.Result.HIGH_RISK, "stub", RUN_TIME);
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
        verifyNoInteractions(model);
    }

    @Test
    public void aRecordWhoseLastErrorIsTerminalIsLeftAlone() {
        screening("s1", "No");
        waitingAre(waiting("s1", ErrorTypeFollowUpStep.Terminal));

        assertTrue(retryWorker.processWaiting());

        verify(errorRecords, never()).delete(any(ErrorRecord.class));
        verifyNoInteractions(model);
    }

    @Test
    public void oneFailedRetryReportsFailureAndTheOthersAreStillTried() throws Exception {
        screening("s1", "No");
        screening("s2", "No");
        when(model.score(any(), anyList())).thenAnswer(invocation -> {
            if (((GeneralEncounter) invocation.getArgument(0)).getUuid().equals("s1")) throw new HighRiskModelException("model down");
            return new ModelResult(ModelResult.Result.LOW_RISK, "stub", RUN_TIME);
        });
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
        verifyNoInteractions(errors, model);
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
        when(model.score(any(), anyList())).thenThrow(new HighRiskModelException("model down"));
        when(errorRecords.findByAvniEntityTypeAndEntityId(AvniEntityType.GeneralEncounter, "s1")).thenReturn(waiting("s1", ErrorTypeFollowUpStep.Process));

        assertThrows(IllegalStateException.class, () -> retryWorker.processError("s1"));

        verify(errorRecords, never()).delete(any(ErrorRecord.class));
    }

    @Test
    public void processErrorSettlesThatScreeningWhenItsRetrySucceeds() throws Exception {
        screening("s1", "No");
        modelSays(ModelResult.Result.LOW_RISK);
        ErrorRecord record = waiting("s1", ErrorTypeFollowUpStep.Process);
        when(errorRecords.findByAvniEntityTypeAndEntityId(AvniEntityType.GeneralEncounter, "s1")).thenReturn(record);

        retryWorker.processError("s1");

        verify(errorRecords).delete(record);
    }
}
