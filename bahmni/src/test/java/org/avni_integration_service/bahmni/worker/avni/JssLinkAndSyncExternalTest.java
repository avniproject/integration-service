package org.avni_integration_service.bahmni.worker.avni;

import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.bahmni.BaseExternalTest;
import org.avni_integration_service.bahmni.BaseSpringTest;
import org.avni_integration_service.bahmni.client.BahmniAvniSessionFactory;
import org.avni_integration_service.bahmni.worker.avni.JssLinkWorker.Report;
import org.avni_integration_service.bahmni.worker.avni.JssLinkWorker.SubjectLink;
import org.avni_integration_service.bahmni.worker.bahmni.atomfeedworker.PatientEncounterEventWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Staged test of JSS linking + visit sync against the live JSS Bahmni prerelease and the Avni org in bahmni-secret.properties.
 * Skipped unless JSS_LIVE_TESTS=true. Uses the local integration DB (avni_int_test) for mappings and error records.
 *
 *   Stage 1, dry run (no writes):  JSS_LIVE_TESTS=true ./gradlew :bahmni:test --tests "*JssLinkAndSyncExternalTest.dryRunAll"
 *   Stage 2, one person:           JSS_LIVE_TESTS=true JSS_CONFIRM_WRITE=yes JSS_BAHMNI_IDS=EME209732 ./gradlew :bahmni:test --tests "*JssLinkAndSyncExternalTest.linkAndSync"
 *   Stage 3/4, several or all:     JSS_BAHMNI_IDS=ID1,ID2  or  JSS_BAHMNI_IDS=ALL
 *
 * Reports are written to bahmni/build/jss-reports/.
 */
@EnabledIfEnvironmentVariable(named = "JSS_LIVE_TESTS", matches = "true")
@SpringBootTest(classes = BaseSpringTest.class)
class JssLinkAndSyncExternalTest extends BaseExternalTest {
    @Autowired
    private JssLinkWorker jssLinkWorker;
    @Autowired
    private PatientEncounterEventWorker patientEncounterEventWorker;
    @Autowired
    private AvniHttpClient avniHttpClient;
    @Autowired
    private BahmniAvniSessionFactory bahmniAvniSessionFactory;

    @BeforeEach
    void beforeEach() {
        avniHttpClient.setAvniSession(bahmniAvniSessionFactory.createSession());
        patientEncounterEventWorker.cacheRunImmutables(getConstants());
    }

    @Test
    void dryRunAll() throws IOException {
        // optional: JSS_BAHMNI_IDS=ID1,ID2 limits the dry run to those IDs
        String wanted = System.getenv("JSS_BAHMNI_IDS");
        List<String> ids = wanted == null || wanted.isBlank() ? List.of("ALL") : List.of(wanted.toUpperCase().split("\\s*,\\s*"));
        List<SubjectLink> links = jssLinkWorker.readIdUpdates(new Date(0)).stream()
                .filter(link -> ids.contains("ALL") || ids.contains(link.bahmniId())).collect(Collectors.toList());
        assertFalse(links.isEmpty(), "No 'Individual ID Update' with Registered = Yes and a Bahmni ID found in Avni");
        writeReport(ids.contains("ALL") ? "dry-run" : "dry-run-" + String.join("_", ids), jssLinkWorker.processIdUpdates(links, true));
    }

    @Test
    void linkAndSync() throws IOException {
        assertTrue("yes".equals(System.getenv("JSS_CONFIRM_WRITE")), "Writes to Bahmni and Avni: set JSS_CONFIRM_WRITE=yes");
        String wanted = System.getenv("JSS_BAHMNI_IDS");
        assertTrue(wanted != null && !wanted.isBlank(), "Set JSS_BAHMNI_IDS to a comma-separated list of Bahmni IDs, or ALL");
        List<String> ids = List.of(wanted.toUpperCase().split("\\s*,\\s*"));

        List<SubjectLink> links = jssLinkWorker.readIdUpdates(new Date(0)).stream()
                .filter(link -> ids.contains("ALL") || ids.contains(link.bahmniId())).collect(Collectors.toList());
        assertFalse(links.isEmpty(), "None of " + ids + " is on an Avni ID Update form");
        writeReport("sync-" + String.join("_", ids), jssLinkWorker.processIdUpdates(links, false));
    }

    private void writeReport(String name, List<Report> reports) throws IOException {
        List<String> out = new ArrayList<>();
        out.add("# JSS link + sync: " + name + " (" + new Date() + ")");
        out.add("");
        out.add("| Bahmni ID | Result | Bahmni visits | Lab | Prescriptions | Nothing to sync | Avni visits | Errors |");
        out.add("|---|---|---|---|---|---|---|---|");
        for (Report r : reports) {
            int avniVisits = r.avniVisitsByForm.values().stream().mapToInt(Integer::intValue).sum();
            out.add(String.format("| %s | %s | %d | %d | %d | %d | %d | %d |", r.bahmniId, r.status, r.visits, r.labVisits, r.prescriptionVisits, r.visitsWithNothingToSync, avniVisits, r.errors.size()));
        }
        for (Report r : reports) {
            out.add("");
            out.add("## " + r.bahmniId + " (Avni " + r.avniSubjectUuid + ")");
            out.add("- Result: " + r.status);
            r.avniVisitsByForm.forEach((form, count) -> out.add("- " + form + ": " + count));
            if (!r.answersWithoutMapping.isEmpty()) out.add("- **Answers without mapping (visit would fail):** " + r.answersWithoutMapping);
            if (!r.questionsNotSynced.isEmpty()) out.add("- Bahmni questions not in any Avni form (skipped): " + r.questionsNotSynced.size() + " concepts");
            r.errors.forEach(e -> out.add("- Error: " + e));
        }
        Path dir = Path.of("build", "jss-reports");
        Files.createDirectories(dir);
        Path file = dir.resolve(name + ".md");
        Files.write(file, out);
        System.out.println(String.join("\n", out));
        System.out.println("Report written to " + file.toAbsolutePath());
    }
}
