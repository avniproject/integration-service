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
import org.avni_integration_service.tanuh.model.HighRiskModelClient;
import org.avni_integration_service.tanuh.model.HighRiskModelException;
import org.avni_integration_service.tanuh.service.TanuhErrorService;
import org.avni_integration_service.tanuh.service.TanuhPhotoDownloader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.io.File;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProcessScreeningTest {
    static final String JOB_USER = "integration@tanuh_uat_local";
    static final String HIGH_RISK_USER = "high-risk@tanuh_uat_local";
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T09:15:00.123Z"), ZoneOffset.UTC);
    static final String CHECK_TIME = "2026-10-07T09:15:00.123Z";

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
    private ErrorRecordRepository errorRecords;
    @Mock
    private IntegratingEntityStatusRepository statuses;
    @TempDir
    File photoDir;
    private final TanuhContextProvider context = new TanuhContextProvider();
    private FakeAvniEncounterRepository encounters;
    private OralScreeningWorker worker;
    private final List<File> downloaded = new ArrayList<>();

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
        worker = new OralScreeningWorker(encounters, subjects, dqs, downloader, model, errors, errorRecords, statuses, context, CLOCK, 10);
        lenient().when(subjects.getSubjectOrThrow(anyString())).thenReturn(patient(false));
        // The patient's registration goes ahead of every screening; the service stores it and the tests leave it be.
        lenient().when(dqs.submit(eq(DqsClient.REGISTRATION), anyMap())).thenReturn(passed());
        lenient().when(downloader.download(anyString())).thenAnswer(invocation -> {
            File file = Files.createTempFile(photoDir.toPath(), "photo", null).toFile();
            downloaded.add(file);
            return file;
        });
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
        for (int i = 0; i < suspiciousPerPhoto.length; i++) rows.add(row("minio://avni-user-media/t/" + uuid + "-" + i + ".jpg", suspiciousPerPhoto[i]));
        return encounters.add(uuid, Map.of("Take photos of all lesions and 1 photo without lesion", rows));
    }

    static DqsResult passed() {
        return new DqsResult(200, true, List.of(), Map.of("pass", 5));
    }

    static DqsResult failedRule() {
        return new DqsResult(200, true, List.of(), Map.of("pass", 4, "fail", 1));
    }

    static Map<String, Object> verdict(String qcStatus) {
        return Map.of("QC status", qcStatus, "QC check time", CHECK_TIME);
    }

    private void dqsSays(DqsResult result) {
        when(dqs.submit(eq(DqsClient.ORAL_SCREENING), anyMap())).thenReturn(result);
    }

    @Test
    public void aScreeningThatPassesQcIsHandedToTheModelAndMarkedPass() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No", "Yes");
        dqsSays(passed());

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        assertEquals(List.of(verdict("QC Pass")), encounters.patchBodies);
        ArgumentCaptor<Map<String, Object>> record = ArgumentCaptor.forClass(Map.class);
        verify(dqs).submit(eq(DqsClient.ORAL_SCREENING), record.capture());
        assertEquals("s1", record.getValue().get("record_id"));
        assertEquals(2, record.getValue().get("image_count"));
        ArgumentCaptor<List<File>> photos = ArgumentCaptor.forClass(List.class);
        verify(model).submit(same(s), photos.capture());
        assertEquals(2, photos.getValue().size());
        verify(downloader).download("minio://avni-user-media/t/s1-0.jpg");
        verify(downloader).download("minio://avni-user-media/t/s1-1.jpg");
        verifyNoInteractions(errors);
    }

    @Test
    public void aScreeningARuleFailsIsMarkedFailAndNotHandedToTheModel() {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        dqsSays(failedRule());

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        assertEquals(List.of(verdict("QC Fail")), encounters.patchBodies);
        verifyNoInteractions(model, downloader, errors);
    }

    @Test
    public void anUnreadableScreeningIsMarkedFail() {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        dqsSays(new DqsResult(422, false, List.of("error IDENTITY_FIELD_MISSING: absent"), Map.of()));

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        assertEquals(List.of(verdict("QC Fail")), encounters.patchBodies);
        verifyNoInteractions(model);
    }

    @Test
    public void aScreeningWithoutPhotosThatPassesIsMarkedPassWithoutAModelCall() {
        GeneralEncounter s = encounters.add("s1", Map.of("Able to Open Mouth?", "No"));
        dqsSays(passed());

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        assertEquals(List.of(verdict("QC Pass")), encounters.patchBodies);
        verifyNoInteractions(model, downloader);
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
        assertEquals(1, encounters.patchBodies.size());
    }

    // A worker's edit synced while the job checked the copy it listed must not be overwritten with a stale verdict.
    @Test
    public void aScreeningChangedWhileBeingCheckedIsNeitherHandedOverNorWritten() {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        when(dqs.submit(eq(DqsClient.ORAL_SCREENING), anyMap())).thenAnswer(invocation -> {
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
        when(dqs.submit(eq(DqsClient.ORAL_SCREENING), anyMap())).thenThrow(new DqsException("DQS cannot take oral_screening s1 right now (503): busy"));

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
        order.verify(dqs).submit(eq(DqsClient.ORAL_SCREENING), anyMap());
        assertEquals("patient-of-s1", registration.getValue().get("record_id"));
        assertEquals("2026-07-03", registration.getValue().get("registered_on"));
    }

    @Test
    public void aRegistrationTheServiceCannotTakeHoldsTheScreeningForARetry() {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        when(dqs.submit(eq(DqsClient.REGISTRATION), anyMap())).thenThrow(new DqsException("DQS cannot take registration p1 right now (503): busy"));

        assertEquals(ScreeningOutcome.FAILED, worker.processScreening(s, true));

        verify(dqs, never()).submit(eq(DqsClient.ORAL_SCREENING), anyMap());
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
        doThrow(new HighRiskModelException("The stand-in model is set to fail")).when(model).submit(any(), anyList());

        assertEquals(ScreeningOutcome.FAILED, worker.processScreening(s, true));

        assertEquals(List.of(verdict("QC Pass")), encounters.patchBodies);
        verify(errors).errorOccurred("s1", "HighRiskModelCallFailed", "HighRiskModelException: The stand-in model is set to fail");
    }

    @Test
    public void theVerdictIsOnTheScreeningBeforeItIsHandedOver() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        dqsSays(passed());
        List<Integer> writtenAtHandOver = new ArrayList<>();
        doAnswer(invocation -> writtenAtHandOver.add(encounters.patchBodies.size())).when(model).submit(any(), anyList());

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        assertEquals(List.of(1), writtenAtHandOver);
    }

    @Test
    public void aPhotoThatWillNotDownloadWaitsAsAHandOverFailure() {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        dqsSays(passed());
        when(downloader.download(anyString())).thenThrow(new IllegalStateException("photo not found"));

        assertEquals(ScreeningOutcome.FAILED, worker.processScreening(s, true));

        assertEquals(List.of(verdict("QC Pass")), encounters.patchBodies);
        verify(errors).errorOccurred("s1", "HighRiskModelCallFailed", "IllegalStateException: photo not found");
        verifyNoInteractions(model);
    }

    @Test
    public void handingOverAgainSendsOnlyAScreeningStillMarkedPass() throws Exception {
        GeneralEncounter marked = screeningWithPhotos("s1", "No");
        marked.addObservation("QC status", "QC Pass");
        GeneralEncounter failed = screeningWithPhotos("s2", "No");
        failed.addObservation("QC status", "QC Fail");

        assertEquals(ScreeningOutcome.WRITTEN, worker.handOverAgain(marked));
        assertEquals(ScreeningOutcome.SKIPPED, worker.handOverAgain(failed));

        verify(model).submit(same(marked), anyList());
        verifyNoMoreInteractions(model);
        verifyNoInteractions(dqs);
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
    public void downloadedPhotosAreDeletedWhetherTheHandOverSucceedsOrFails() throws Exception {
        dqsSays(passed());
        doNothing().doThrow(new HighRiskModelException("down")).when(model).submit(any(), anyList());

        worker.processScreening(screeningWithPhotos("ok", "No", "No"), true);
        worker.processScreening(screeningWithPhotos("down", "No"), true);

        assertEquals(3, downloaded.size());
        downloaded.forEach(file -> assertFalse(file.exists(), file + " should be deleted"));
    }
}
