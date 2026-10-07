package org.avni_integration_service.tanuh.worker;

import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.avni.repository.AvniSubjectRepository;
import org.avni_integration_service.integration_data.domain.AvniEntityType;
import org.avni_integration_service.integration_data.domain.IntegratingEntityStatus;
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
import org.avni_integration_service.tanuh.service.RoutingPolicy;
import org.avni_integration_service.tanuh.service.Sampler;
import org.avni_integration_service.tanuh.service.TanuhErrorService;
import org.avni_integration_service.tanuh.service.TanuhPhotoDownloader;
import org.avni_integration_service.util.FormatAndParseUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Timeout(value = 10, unit = TimeUnit.SECONDS)
public class ProcessNewTest {
    static final String JOB_USER = "integration@tanuh_uat_local";
    static final Date START = Date.from(Instant.parse("2026-10-07T00:00:00Z"));

    @Mock
    private AvniSubjectRepository subjects;
    @Mock
    private TanuhPhotoDownloader downloader;
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
    private final CountingModel model = new CountingModel();
    private final List<Date> savedCursors = new ArrayList<>();
    private FakeAvniEncounterRepository encounters;
    private IntegratingEntityStatus cursor;
    private OralScreeningWorker worker;

    static class CountingModel implements HighRiskModelClient {
        final Map<String, Integer> calls = new HashMap<>();
        ModelResult.Result answer = ModelResult.Result.LOW_RISK;

        @Override
        public ModelResult score(GeneralEncounter screening, List<File> photos) {
            calls.merge(screening.getUuid(), 1, Integer::sum);
            return new ModelResult(answer, "stub", Instant.parse("2026-10-07T09:15:00.123Z"));
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        IntegrationSystemConfig user = new IntegrationSystemConfig();
        user.setKey("avni_user");
        user.setValue(JOB_USER);
        IntegrationSystem system = new IntegrationSystem();
        system.setId(15);
        ReflectionTestUtils.setField(system, "name", "tanuh_uat_local");
        context.set(new TanuhConfig(new IntegrationSystemConfigCollection(List.of(user)), system));
        encounters = new FakeAvniEncounterRepository(JOB_USER, START);
        cursor = new IntegratingEntityStatus();
        cursor.setEntityType("TanuhOralScreening");
        cursor.setReadUptoDateTime(START);
        lenient().when(statuses.find("TanuhOralScreening")).thenReturn(cursor);
        lenient().when(statuses.save(any())).thenAnswer(invocation -> {
            savedCursors.add(((IntegratingEntityStatus) invocation.getArgument(0)).getReadUptoDateTime());
            return invocation.getArgument(0);
        });
        Subject live = new Subject();
        live.setVoided(false);
        lenient().when(subjects.getSubjectOrThrow(anyString())).thenReturn(live);
        lenient().when(downloader.download(anyString())).thenAnswer(invocation -> Files.createTempFile(photoDir.toPath(), "photo", null).toFile());
        worker = new OralScreeningWorker(encounters, subjects, downloader, model, new RoutingPolicy(), sampler, errors, errorRecords, statuses, context, 10);
    }

    @AfterEach
    public void tearDown() {
        TanuhContextProvider.clear();
    }

    private static Map<String, Object> photos(String uuid) {
        Map<String, Object> row = new HashMap<>();
        row.put("Oral Image", "minio://avni-user-media/t/" + uuid + ".jpg");
        row.put("Suspicious Lesion?", "No");
        return Map.of("Take photos of all lesions and 1 photo without lesion", List.of(row));
    }

    private void addScreenings(int count) {
        for (int i = 1; i <= count; i++) encounters.add(String.format("s%02d", i), photos(String.format("s%02d", i)));
    }

    private void assertEachSentOnce(int count) {
        assertEquals(count, model.calls.size());
        model.calls.forEach((uuid, calls) -> assertEquals(1, calls, uuid));
    }

    @Test
    public void twentyFiveScreeningsArePagedByTenEachSentOnceAndTheRunEnds() {
        addScreenings(25);
        worker.processNew();
        assertEachSentOnce(25);
        assertEquals(25, encounters.patchedUuids.size());
    }

    @Test
    public void threeRunsScoreEachScreeningOncePerWorkerSave() {
        addScreenings(25);
        worker.processNew();
        worker.processNew();
        worker.processNew();
        assertEachSentOnce(25);
        assertEquals(25, encounters.patchedUuids.size());
    }

    @Test
    public void aWorkerEditAfterScoringScoresItAgainAndReplacesAllFive() {
        addScreenings(3);
        model.answer = ModelResult.Result.NOT_SUSPICIOUS;
        worker.processNew();
        encounters.workerEdits("s02");
        model.answer = ModelResult.Result.HIGH_RISK;

        worker.processNew();

        assertEquals(2, model.calls.get("s02"));
        assertEquals(1, model.calls.get("s01"));
        GeneralEncounter s02 = encounters.screenings.get("s02");
        assertEquals("High Risk", s02.getObservation("High risk model result"));
        assertEquals("High Risk", s02.getObservation("Review category"));
        verify(sampler, times(1)).draw("s02");
    }

    @Test
    public void aClearedCaseGetsAFreshDrawWhenScoredAgain() {
        addScreenings(1);
        model.answer = ModelResult.Result.NOT_SUSPICIOUS;
        when(sampler.draw("s01")).thenReturn(false, true);

        worker.processNew();
        assertEquals("Closed", encounters.screenings.get("s01").getObservation("Review category"));
        encounters.workerEdits("s01");
        worker.processNew();

        assertEquals("Safety sample", encounters.screenings.get("s01").getObservation("Review category"));
        verify(sampler, times(2)).draw("s01");
    }

    @Test
    public void aNotScoredScreeningEditedToAddPhotosIsSent() {
        encounters.add("bare", Map.of("Able to Open Mouth?", "No"));
        worker.processNew();
        assertEquals("Not scored", encounters.screenings.get("bare").getObservation("Review category"));
        assertTrue(model.calls.isEmpty());

        photos("bare").forEach(encounters.screenings.get("bare")::addObservation);
        encounters.workerEdits("bare");
        worker.processNew();

        assertEquals(1, model.calls.get("bare"));
        assertEquals("Scored", encounters.screenings.get("bare").getObservation("High risk model status"));
    }

    @Test
    public void anOpenErrorRecordIsNotSentEvenWhenTheWorkerEditedItLast() {
        addScreenings(3);
        lenient().when(errorRecords.findByAvniEntityTypeAndEntityId(AvniEntityType.GeneralEncounter, "s02")).thenReturn(new ErrorRecord());
        encounters.workerEdits("s02");

        worker.processNew();

        assertNull(model.calls.get("s02"));
        assertEquals(2, model.calls.size());
    }

    @Test
    public void aRunStoppedPartWayResumesWithoutResending() {
        addScreenings(25);
        encounters.failOnListCall = 2;

        assertThrows(RuntimeException.class, () -> worker.processNew());
        assertEquals(10, model.calls.size());
        worker.processNew();

        assertEachSentOnce(25);
    }

    @Test
    public void aScreeningAtTheCursorsExactMillisecondIsProcessedOnce() {
        addScreenings(2);
        cursor.setReadUptoDateTime(encounters.screenings.get("s02").getLastModifiedDate());

        worker.processNew();
        worker.processNew();

        assertEquals(Map.of("s01", 1, "s02", 1), model.calls);
    }

    @Test
    public void exactlyAPageSizeOfRowsFetchesTheNextPageAndEnds() {
        addScreenings(10);
        worker.processNew();
        assertEachSentOnce(10);
        assertTrue(encounters.listCalls >= 2, "a full page must be followed by another read");
    }

    // The cursor follows the rows as listed; the job's own writes come later and are re-read and skipped next run.
    @Test
    public void theCursorIsSavedAfterEachScreeningAtItsListedTimeAndOnlyMovesForward() {
        addScreenings(5);
        List<String> listedTimes = encounters.screenings.values().stream()
                .map(s -> FormatAndParseUtil.toISODateTimeString(s.getLastModifiedDate())).toList();

        worker.processNew();

        assertEquals(listedTimes, savedCursors.stream().map(FormatAndParseUtil::toISODateTimeString).toList());
        for (int i = 1; i < savedCursors.size(); i++)
            assertTrue(savedCursors.get(i).after(savedCursors.get(i - 1)), "cursor moved back at save " + i);
    }

    @Test
    public void aMissingCursorRowFailsTheRun() {
        when(statuses.find("TanuhOralScreening")).thenReturn(null);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> worker.processNew());
        assertTrue(e.getMessage().contains("TanuhOralScreening") && e.getMessage().contains("setup script"), e.getMessage());
    }
}
