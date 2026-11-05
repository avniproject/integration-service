package org.avni_integration_service.tanuh.worker;

import org.avni_integration_service.tanuh.domain.TanuhConceptsFixture;
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
import org.avni_integration_service.tanuh.dqs.DqsClient;
import org.avni_integration_service.tanuh.dqs.DqsResult;
import org.avni_integration_service.tanuh.model.HighRiskModelClient;
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
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Supplier;
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
    private DqsClient dqs;
    @Mock
    private HighRiskModelClient model;
    @Mock
    private TanuhErrorService errors;
    @Mock
    private ErrorRecordRepository errorRecords;
    @Mock
    private IntegratingEntityStatusRepository statuses;
    @TempDir
    File photoDir;
    private final TanuhContextProvider context = new TanuhContextProvider();
    private final Map<String, Integer> sent = new HashMap<>();
    private final Map<String, Runnable> duringNextSend = new HashMap<>();
    private DqsResult answer = ProcessScreeningTest.passed();
    private final List<Date> savedCursors = new ArrayList<>();
    private FakeAvniEncounterRepository encounters;
    private IntegratingEntityStatus cursor;
    private OralScreeningWorker worker;

    @BeforeEach
    public void setUp() throws Exception {
        // The patient's registration goes ahead of every screening; the service stores it and the tests leave it be.
        lenient().when(dqs.submit(eq(DqsClient.REGISTRATION), anyMap())).thenReturn(ProcessScreeningTest.passed());
        IntegrationSystemConfig user = new IntegrationSystemConfig();
        user.setKey("avni_user");
        user.setValue(JOB_USER);
        IntegrationSystem system = new IntegrationSystem();
        system.setId(15);
        ReflectionTestUtils.setField(system, "name", "tanuh_uat_local");
        context.set(new TanuhConfig(new IntegrationSystemConfigCollection(List.of(user)), system));
        context.setConcepts(TanuhConceptsFixture.defaults());
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
        lenient().when(dqs.submit(eq(DqsClient.ORAL_SCREENING), any(Supplier.class))).thenAnswer(invocation -> {
            String uuid = (String) ((Supplier<Map<?, ?>>) invocation.getArgument(1)).get().get("record_id");
            sent.merge(uuid, 1, Integer::sum);
            Runnable during = duringNextSend.remove(uuid);
            if (during != null) during.run();
            return answer;
        });
        worker = new OralScreeningWorker(encounters, subjects, dqs, downloader, model, errors, errorRecords, statuses, context, new TickingClock(), 10);
    }

    // A millisecond later on every read, as a real clock would be between writes.
    static class TickingClock extends Clock {
        private Instant now = Instant.parse("2026-10-07T09:15:00.000Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            now = now.plusMillis(1);
            return now;
        }
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
        assertEquals(count, sent.size());
        sent.forEach((uuid, calls) -> assertEquals(1, calls, uuid));
    }

    @Test
    public void twentyFiveScreeningsArePagedByTenEachSentOnceAndTheRunEnds() {
        addScreenings(25);
        worker.processNew();
        assertEachSentOnce(25);
        assertEquals(25, encounters.patchedUuids.size());
    }

    @Test
    public void threeRunsSendEachScreeningOncePerWorkerSave() {
        addScreenings(25);
        worker.processNew();
        worker.processNew();
        worker.processNew();
        assertEachSentOnce(25);
        assertEquals(25, encounters.patchedUuids.size());
    }

    @Test
    public void aWorkerEditAfterTheCheckSendsItAgainAndReplacesTheVerdict() throws Exception {
        addScreenings(3);
        worker.processNew();
        assertEquals("QC Pass", encounters.screenings.get("s02").getObservation("QC status"));
        encounters.workerEdits("s02");
        answer = ProcessScreeningTest.failedRule();

        worker.processNew();

        assertEquals(2, sent.get("s02"));
        assertEquals(1, sent.get("s01"));
        assertEquals("QC Fail", encounters.screenings.get("s02").getObservation("QC status"));
        verify(model, times(3)).submit(any(), anyList());
    }

    @Test
    public void aScreeningWithoutPhotosEditedToAddThemIsHandedToTheModel() throws Exception {
        encounters.add("bare", Map.of("Able to Open Mouth?", "No"));
        worker.processNew();
        assertEquals("QC Pass", encounters.screenings.get("bare").getObservation("QC status"));
        verifyNoInteractions(model);

        photos("bare").forEach(encounters.screenings.get("bare")::addObservation);
        encounters.workerEdits("bare");
        worker.processNew();

        assertEquals(2, sent.get("bare"));
        verify(model).submit(same(encounters.screenings.get("bare")), anyList());
    }

    // Review finding: a full page that could not move past "from" jumped to its last row's time, and the server's
    // exclusive lower bound then hid every other row in that millisecond.
    @Test
    public void skippedScreeningsFillingPagesInOneMillisecondDoNotHideTheRestOfIt() {
        List<String> all = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            String uuid = String.format("a%02d", i);
            encounters.add(uuid, photos(uuid)).setVoided(true);
            all.add(uuid);
        }
        for (String uuid : List.of("b1", "b2", "b3")) {
            encounters.add(uuid, photos(uuid));
            all.add(uuid);
        }
        encounters.stampTogether(all);

        worker.processNew();

        assertEquals(Map.of("b1", 1, "b2", 1, "b3", 1), sent);
    }

    @Test
    public void thirtyFiveScreeningsInOneMillisecondAreEachSentOnce() {
        addScreenings(35);
        encounters.stampTogether(new ArrayList<>(encounters.screenings.keySet()));

        worker.processNew();
        worker.processNew();
        worker.processNew();

        assertEachSentOnce(35);
        assertEquals(35, encounters.patchedUuids.size());
    }

    // Review finding: an edit synced while the job checked the copy it listed was overwritten, then skipped as the job's own.
    @Test
    public void anEditLandingWhileAScreeningIsCheckedIsCheckedOnTheNextRun() {
        addScreenings(1);
        duringNextSend.put("s01", () -> {
            encounters.screenings.get("s01").addObservation("Any Symptoms", "Yes");
            encounters.workerEdits("s01");
        });

        worker.processNew();
        worker.processNew();

        assertEquals(2, sent.get("s01"));
        assertEquals(List.of("s01"), encounters.patchedUuids);
        assertEquals("QC Pass", encounters.screenings.get("s01").getObservation("QC status"));
    }

    // The check time makes every write a change, so the job is the last editor after each one and the screening rests.
    @Test
    public void aScreeningEditedOnceIsWrittenOnceMoreAndThenLeftAlone() {
        encounters.add("bare", Map.of("Able to Open Mouth?", "No"));
        worker.processNew();
        encounters.screenings.get("bare").addObservation("Referral is required due to limited mouth opening", "No");
        encounters.workerEdits("bare");

        worker.processNew();
        worker.processNew();
        worker.processNew();

        assertEquals(List.of("bare", "bare"), encounters.patchedUuids);
        assertEquals(2, sent.get("bare"));
    }

    @Test
    public void anOpenErrorRecordIsNotSentEvenWhenTheWorkerEditedItLast() {
        addScreenings(3);
        lenient().when(errorRecords.findByAvniEntityTypeAndEntityId(AvniEntityType.GeneralEncounter, "s02")).thenReturn(new ErrorRecord());
        encounters.workerEdits("s02");

        worker.processNew();

        assertNull(sent.get("s02"));
        assertEquals(2, sent.size());
    }

    @Test
    public void aRunStoppedPartWayResumesWithoutResending() {
        addScreenings(25);
        encounters.failOnListCall = 2;

        assertThrows(RuntimeException.class, () -> worker.processNew());
        assertEquals(10, sent.size());
        worker.processNew();

        assertEachSentOnce(25);
    }

    @Test
    public void aScreeningAtTheCursorsExactMillisecondIsProcessedOnce() {
        addScreenings(2);
        cursor.setReadUptoDateTime(encounters.screenings.get("s02").getLastModifiedDate());

        worker.processNew();
        worker.processNew();

        assertEquals(Map.of("s01", 1, "s02", 1), sent);
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

    // A cursor in the future reads nothing, run after run, with the health check green. It must fail instead.
    @Test
    public void aCursorFarInTheFutureFailsTheRunBeforeReading() {
        addScreenings(2);
        cursor.setReadUptoDateTime(FormatAndParseUtil.fromAvniDateTime("2202-10-07T09:32:11.791Z"));

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> worker.processNew());

        assertTrue(e.getMessage().contains("future"), e.getMessage());
        assertEquals(0, encounters.listCalls);
        assertTrue(sent.isEmpty());
    }

    @Test
    public void aScreeningDatedFarInTheFutureFailsTheRunAndLeavesTheCursor() {
        addScreenings(3);
        Map<String, Object> audit = new HashMap<>();
        audit.put("Last modified at", "2202-10-07T09:32:11.791Z");
        audit.put("Last modified by", FakeAvniEncounterRepository.WORKER);
        encounters.screenings.get("s03").set("audit", audit);
        String s02Time = FormatAndParseUtil.toISODateTimeString(encounters.screenings.get("s02").getLastModifiedDate());

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> worker.processNew());

        assertTrue(e.getMessage().contains("s03") && e.getMessage().contains("future"), e.getMessage());
        assertNull(sent.get("s03"));
        assertEquals(s02Time, FormatAndParseUtil.toISODateTimeString(cursor.getReadUptoDateTime()));
    }

    @Test
    public void aMissingCursorRowFailsTheRun() {
        when(statuses.find("TanuhOralScreening")).thenReturn(null);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> worker.processNew());
        assertTrue(e.getMessage().contains("TanuhOralScreening") && e.getMessage().contains("setup script"), e.getMessage());
    }
}
