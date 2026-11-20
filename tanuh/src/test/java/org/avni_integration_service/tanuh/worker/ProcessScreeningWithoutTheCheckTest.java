package org.avni_integration_service.tanuh.worker;

import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.avni.repository.AvniSubjectRepository;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfig;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.avni_integration_service.integration_data.repository.ErrorRecordRepository;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.avni_integration_service.tanuh.config.TanuhConfig;
import org.avni_integration_service.tanuh.config.TanuhContextProvider;
import org.avni_integration_service.tanuh.domain.TanuhConceptsFixture;
import org.avni_integration_service.tanuh.domain.TanuhConcepts.Concept;
import org.avni_integration_service.tanuh.dqs.DqsClient;
import org.avni_integration_service.tanuh.model.HighRiskModelClient;
import org.avni_integration_service.tanuh.model.HighRiskModelException;
import org.avni_integration_service.tanuh.model.Submission;
import org.avni_integration_service.tanuh.service.PhotoOutsideStorageException;
import org.avni_integration_service.tanuh.service.SentScreenings;
import org.avni_integration_service.tanuh.service.TanuhAlerts;
import org.avni_integration_service.tanuh.service.TanuhErrorService;
import org.avni_integration_service.tanuh.service.TanuhPhotoDownloader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.avni_integration_service.tanuh.worker.ProcessScreeningTest.CLOCK;
import static org.avni_integration_service.tanuh.worker.ProcessScreeningTest.JOB_USER;
import static org.avni_integration_service.tanuh.worker.ProcessScreeningTest.PHOTOS;
import static org.avni_integration_service.tanuh.worker.ProcessScreeningTest.callMade;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// With dqs_enabled false the data quality check is skipped (Adam, 17 Nov 2026): every completed Oral Screening with a
// photo Tanuh's model can read is sent while OCSHR Call Made is empty, and the job writes OCSHR Call Made and nothing else.
@ExtendWith(MockitoExtension.class)
public class ProcessScreeningWithoutTheCheckTest {
    private static final List<String> TWO_PHOTOS = List.of("s3://prod-user-media/tanuh_uat/s1-0.jpg", "s3://prod-user-media/tanuh_uat/s1-1.jpg");

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
    private final FakeSentScreenings sent = new FakeSentScreenings();
    private FakeAvniEncounterRepository encounters;
    private OralScreeningWorker worker;

    @BeforeEach
    public void setUp() throws Exception {
        IntegrationSystem system = new IntegrationSystem();
        system.setId(15);
        ReflectionTestUtils.setField(system, "name", "tanuh_uat_local");
        context.set(new TanuhConfig(new IntegrationSystemConfigCollection(List.of(config("avni_user", JOB_USER),
                config("high_risk_avni_user", ProcessScreeningTest.HIGH_RISK_USER), config("dqs_enabled", "false"))), system));
        context.setConcepts(TanuhConceptsFixture.defaults());
        encounters = new FakeAvniEncounterRepository(JOB_USER, Date.from(Instant.parse("2026-10-07T00:00:00Z")));
        worker = new OralScreeningWorker(encounters, subjects, dqs, downloader, model, errors, alerts, sent, errorRecords, statuses, context, CLOCK, 10);
        lenient().when(subjects.getSubjectOrThrow(anyString())).thenReturn(new Subject());
        lenient().when(downloader.sign(anyString())).thenAnswer(invocation -> invocation.getArgument(0) + "?X-Amz-Signature=signed");
        lenient().when(model.submit(anyString(), anyList(), any())).thenReturn(new Submission(Submission.Outcome.ACCEPTED, "{}"));
    }

    @AfterEach
    public void tearDown() {
        TanuhContextProvider.clear();
    }

    private static IntegrationSystemConfig config(String key, String value) {
        IntegrationSystemConfig c = new IntegrationSystemConfig();
        c.setKey(key);
        c.setValue(value);
        return c;
    }

    private GeneralEncounter screeningWithPhotos(String uuid, int photos) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < photos; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("Oral Image", "https://s3.ap-south-1.amazonaws.com/prod-user-media/tanuh_uat/" + uuid + "-" + i + ".jpg");
            row.put("Suspicious Lesion?", "No");
            rows.add(row);
        }
        return encounters.add(uuid, Map.of(PHOTOS, rows));
    }

    private Optional<Concept> recordedAnswer(String uuid) {
        return sent.find(uuid).flatMap(SentScreenings.SentScreening::callMade);
    }

    @Test
    public void aScreeningWithPhotosIsSentWithoutTheCheckAndOnlyCallMadeIsWritten() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", 2);

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        verifyNoInteractions(dqs);
        verify(model).submit(eq("s1"), eq(TWO_PHOTOS), any());
        assertEquals(List.of(callMade("Yes")), encounters.patchBodies);
        assertEquals(Optional.of(Concept.YES), recordedAnswer("s1"));
    }

    @Test
    public void aScreeningWithNoPhotoIsMarkedNoWithoutTheCheck() throws Exception {
        GeneralEncounter s = encounters.add("s1", Map.of("Able to Open Mouth?", "No"));

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        verifyNoInteractions(dqs, model);
        assertEquals(List.of(callMade("No")), encounters.patchBodies);
    }

    @Test
    public void aPhotoAvniRefusesIsMarkedNoAndReportedWithoutTheCheck() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", 1);
        when(downloader.sign(anyString())).thenThrow(new PhotoOutsideStorageException("outside", null));

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(s, true));

        verifyNoInteractions(dqs);
        verify(model, never()).submit(anyString(), anyList(), any());
        assertEquals(List.of(callMade("No")), encounters.patchBodies);
        verify(alerts).screeningNeedsAPerson(eq("s1"), eq(TanuhAlerts.Reason.PHOTO_OUTSIDE_STORAGE), anyString());
    }

    // A worker's edit of a screening already marked is left alone: no call, no check, nothing written.
    @Test
    public void aScreeningAlreadyMarkedIsLeftAloneAfterAnEdit() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", 2);
        s.addObservation("OCSHR Call Made", "Yes");
        encounters.workerEdits("s1");

        assertEquals(ScreeningOutcome.SKIPPED, worker.processScreening(s, true));

        verifyNoInteractions(dqs, model);
        assertTrue(encounters.patchBodies.isEmpty());
    }

    // A phone that last synced before the job ran clears OCSHR Call Made when the worker edits the screening. The job's
    // record of sends puts the Yes back, and the screening is not sent again.
    @Test
    public void aYesAPhoneClearedIsWrittenBackWithoutASecondSend() throws Exception {
        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(screeningWithPhotos("s1", 2), true));
        GeneralEncounter edited = encounters.screenings.get("s1");
        edited.getObservations().remove("OCSHR Call Made");
        encounters.workerEdits("s1");

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(edited, true));

        verify(model, times(1)).submit(anyString(), anyList(), any());
        verifyNoInteractions(dqs);
        assertEquals(List.of(callMade("Yes"), callMade("Yes")), encounters.patchBodies);
    }

    // A call that fails writes nothing. The retry job sends the same request again, and Tanuh's answer is written.
    @Test
    public void aFailedCallIsSentAgainByTheRetryAndMarkedYes() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", 2);
        when(model.submit(anyString(), anyList(), any()))
                .thenThrow(new HighRiskModelException("Model service at tanuh did not answer: Read timed out"))
                .thenReturn(new Submission(Submission.Outcome.ACCEPTED, "{}"));
        assertEquals(ScreeningOutcome.FAILED, worker.processScreening(s, true));
        verify(errors).errorOccurred(eq("s1"), eq(TanuhErrorService.MODEL_CALL_FAILED), contains("Read timed out"));
        assertTrue(encounters.patchBodies.isEmpty());

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(encounters.screenings.get("s1"), false));

        verify(model, times(2)).submit(eq("s1"), eq(TWO_PHOTOS), any());
        verifyNoInteractions(dqs);
        assertEquals(List.of(callMade("Yes")), encounters.patchBodies);
    }

    // A worker's edit synced while Tanuh answered: the Yes is kept in the job's record but not written over the edit, and
    // the newer version gets it on its next listing without a second call.
    @Test
    public void anEditDuringTheCallKeepsTheYesForTheNewerVersion() throws Exception {
        GeneralEncounter s = screeningWithPhotos("s1", 2);
        when(model.submit(anyString(), anyList(), any())).thenAnswer(invocation -> {
            encounters.workerEdits("s1");
            return new Submission(Submission.Outcome.ACCEPTED, "{}");
        });

        assertEquals(ScreeningOutcome.CHANGED, worker.processScreening(s, true));
        assertTrue(encounters.patchBodies.isEmpty());
        assertEquals(Optional.of(Concept.YES), recordedAnswer("s1"));

        assertEquals(ScreeningOutcome.WRITTEN, worker.processScreening(encounters.screenings.get("s1"), true));

        verify(model, times(1)).submit(anyString(), anyList(), any());
        assertEquals(List.of(callMade("Yes")), encounters.patchBodies);
    }
}
