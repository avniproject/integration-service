package org.avni_integration_service.tanuh.worker;

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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProcessScreeningTest {
    static final String JOB_USER = "integration@tanuh_uat_local";
    static final Instant RUN_TIME = Instant.parse("2026-10-07T09:15:00.123Z");

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
    private OralScreeningWorker worker;
    private final List<File> downloaded = new ArrayList<>();

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
        worker = new OralScreeningWorker(encounters, subjects, downloader, model, new RoutingPolicy(), sampler, errors, errorRecords, statuses, context, 10);
        lenient().when(subjects.getSubjectOrThrow(anyString())).thenReturn(patient(false));
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

    private void modelSays(ModelResult.Result result) throws Exception {
        when(model.score(any(), anyList())).thenReturn(new ModelResult(result, "stub", RUN_TIME));
    }

    @Test
    public void aCompletedScreeningWithPhotosIsScoredAndOnlyTheFiveValuesAreSent() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No", "Yes");
        modelSays(ModelResult.Result.HIGH_RISK);

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        assertEquals(List.of(Map.of(
                "High risk model result", "High Risk",
                "High risk model status", "Scored",
                "High risk model version", "stub",
                "High risk model run time", "2026-10-07T09:15:00.123Z",
                "Review category", "High Risk")), encounters.patchBodies);
        ArgumentCaptor<List<File>> photos = ArgumentCaptor.forClass(List.class);
        verify(model).score(same(s), photos.capture());
        assertEquals(2, photos.getValue().size());
        verify(downloader).download("minio://avni-user-media/t/s1-0.jpg");
        verify(downloader).download("minio://avni-user-media/t/s1-1.jpg");
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
        verifyNoInteractions(model, subjects);
    }

    @Test
    public void aDeletedPatientIsSkipped() {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        when(subjects.getSubjectOrThrow("patient-of-s1")).thenReturn(patient(true));

        assertEquals(ScreeningOutcome.SKIPPED, worker.processScreening(s, true));
        assertTrue(encounters.patchBodies.isEmpty());
        verifyNoInteractions(model);
    }

    @Test
    public void lastChangedByTheJobIsSkippedWhateverTheCase() {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        encounters.touch(s, "INTEGRATION@Tanuh_UAT_Local");

        assertEquals(ScreeningOutcome.SKIPPED, worker.processScreening(s, true));
        verifyNoInteractions(model, subjects);
    }

    @Test
    public void anOpenErrorRecordIsSkippedOnlyWhenAsked() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        lenient().when(errorRecords.findByAvniEntityTypeAndEntityId(AvniEntityType.GeneralEncounter, "s1")).thenReturn(new ErrorRecord());
        modelSays(ModelResult.Result.LOW_RISK);

        assertEquals(ScreeningOutcome.SKIPPED, worker.processScreening(s, true));
        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, false));
        assertEquals(1, encounters.patchBodies.size());
    }

    @Test
    public void noPhotoIsMarkedNotScoredWithNullsAndNoModelCall() {
        GeneralEncounter s = encounters.add("s1", Map.of("Able to Open Mouth?", "No"));

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        Map<String, Object> expected = new HashMap<>();
        expected.put("High risk model status", "Not scored");
        expected.put("Review category", "Not scored");
        expected.put("High risk model result", null);
        expected.put("High risk model version", null);
        expected.put("High risk model run time", null);
        assertEquals(List.of(expected), encounters.patchBodies);
        verifyNoInteractions(model, errors);
    }

    // A worker's edit synced while the job scored the copy it listed must not be overwritten with a stale result.
    @Test
    public void aScreeningChangedWhileBeingScoredIsNotWritten() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        when(model.score(any(), anyList())).thenAnswer(invocation -> {
            encounters.workerEdits("s1");
            return new ModelResult(ModelResult.Result.HIGH_RISK, "stub", RUN_TIME);
        });

        assertEquals(ScreeningOutcome.CHANGED, worker.processScreening(s, true));

        assertTrue(encounters.patchedUuids.isEmpty());
        verifyNoInteractions(errors);
    }

    @Test
    public void aScreeningChangedBeforeItIsMarkedNotScoredIsNotWritten() {
        GeneralEncounter s = encounters.add("s1", Map.of("Able to Open Mouth?", "No"));
        when(subjects.getSubjectOrThrow(anyString())).thenAnswer(invocation -> {
            encounters.workerEdits("s1");
            return patient(false);
        });

        assertEquals(ScreeningOutcome.CHANGED, worker.processScreening(s, true));

        assertTrue(encounters.patchedUuids.isEmpty());
        verifyNoInteractions(model, errors);
    }

    // Writing the same values again changes nothing on the server, so the worker would stay the last editor.
    @Test
    public void noPhotoAlreadyMarkedNotScoredIsSkippedWithoutAWrite() {
        Map<String, Object> observations = new HashMap<>();
        observations.put("Able to Open Mouth?", "No");
        observations.put("High risk model status", "Not scored");
        observations.put("Review category", "Not scored");
        GeneralEncounter s = encounters.add("s1", observations);

        assertEquals(ScreeningOutcome.SKIPPED, worker.processScreening(s, true));

        assertTrue(encounters.patchedUuids.isEmpty());
        verifyNoInteractions(model, errors);
    }

    @Test
    public void aModelFailureWritesNothingAndWaitsForARetry() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", "No");
        when(model.score(any(), anyList())).thenThrow(new HighRiskModelException("The stand-in model is set to fail"));

        assertEquals(ScreeningOutcome.FAILED, worker.processScreening(s, true));

        assertTrue(encounters.patchBodies.isEmpty());
        verify(errors).errorOccurred("s1", "HighRiskModelCallFailed", "HighRiskModelException: The stand-in model is set to fail");
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
    public void downloadedPhotosAreDeletedWhetherTheModelSucceedsOrFails() throws Exception {
        when(model.score(any(), anyList()))
                .thenReturn(new ModelResult(ModelResult.Result.LOW_RISK, "stub", RUN_TIME))
                .thenThrow(new HighRiskModelException("down"));

        worker.processScreening(screeningWithPhotos("ok", "No", "No"), true);
        worker.processScreening(screeningWithPhotos("down", "No"), true);

        assertEquals(3, downloaded.size());
        downloaded.forEach(file -> assertFalse(file.exists(), file + " should be deleted"));
    }

    @Test
    public void routesWithTheWorkersOpinionAndDrawsOnlyForAClearedCase() throws Exception {
        modelSays(ModelResult.Result.NOT_SUSPICIOUS);
        when(sampler.draw("picked")).thenReturn(true);
        when(sampler.draw("cleared")).thenReturn(false);

        worker.processScreening(screeningWithPhotos("flagged", "No", "Yes"), true);
        worker.processScreening(screeningWithPhotos("picked", "No"), true);
        worker.processScreening(screeningWithPhotos("cleared", "No"), true);

        assertEquals(List.of("FLW override", "Safety sample", "Closed"),
                encounters.patchBodies.stream().map(body -> body.get("Review category")).toList());
        verify(sampler, never()).draw("flagged");
    }

    @Test
    public void theEdgeVerdictChangesNothing() throws Exception {
        modelSays(ModelResult.Result.LOW_RISK);
        GeneralEncounter edgeSuspicious = screeningWithPhotos("e1", "No");
        edgeSuspicious.addObservation("Image-wise AI Assessment", List.of(Map.of("AI Verdict", "Suspicious")));
        GeneralEncounter edgeClear = screeningWithPhotos("e2", "No");
        edgeClear.addObservation("Image-wise AI Assessment", List.of(Map.of("AI Verdict", "Not suspicious")));
        GeneralEncounter noEdge = screeningWithPhotos("e3", "No");

        for (GeneralEncounter s : List.of(edgeSuspicious, edgeClear, noEdge)) worker.processScreening(s, true);

        assertEquals(3, encounters.patchBodies.size());
        assertEquals(encounters.patchBodies.get(0), encounters.patchBodies.get(1));
        assertEquals(encounters.patchBodies.get(0), encounters.patchBodies.get(2));
    }
}
