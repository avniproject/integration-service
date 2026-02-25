package org.avni_integration_service.bahmni.worker.avni;

import org.avni_integration_service.integration_data.domain.Constants;
import org.avni_integration_service.integration_data.repository.ConstantsRepository;
import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.GeneralEncountersResponse;
import org.avni_integration_service.avni.domain.ProgramEncounter;
import org.avni_integration_service.avni.domain.ProgramEncountersResponse;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.avni.repository.AvniEnrolmentRepository;
import org.avni_integration_service.avni.repository.AvniEncounterRepository;
import org.avni_integration_service.avni.repository.AvniProgramEncounterRepository;
import org.avni_integration_service.avni.repository.AvniSubjectRepository;

import java.util.HashMap;
import org.avni_integration_service.bahmni.client.BahmniAvniSessionFactory;
import org.avni_integration_service.bahmni.service.MappingMetaDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.avni_integration_service.bahmni.BaseSpringTest;

import java.util.List;

@SpringBootTest(classes = BaseSpringTest.class)
class ProgramEncounterWorkerExternalTest {
    @Autowired
    private ProgramEncounterWorker programEncounterWorker;
    @Autowired
    private GeneralEncounterWorker generalEncounterWorker;
    @Autowired
    private EnrolmentWorker enrolmentWorker;
    @Autowired
    private SubjectWorker subjectWorker;
    @Autowired
    ConstantsRepository constantsRepository;
    @Autowired
    AvniProgramEncounterRepository programEncounterRepository;
    @Autowired
    AvniEncounterRepository encounterRepository;
    @Autowired
    MappingMetaDataService mappingMetaDataService;
    @Autowired
    AvniEnrolmentRepository avniEnrolmentRepository;
    @Autowired
    AvniSubjectRepository avniSubjectRepository;
    @Autowired
    private AvniHttpClient avniHttpClient;
    @Autowired
    private BahmniAvniSessionFactory bahmniAvniSessionFactory;

    @BeforeEach
    public void beforeEach() {
        avniHttpClient.setAvniSession(bahmniAvniSessionFactory.createSession());
    }

    @Test
    @Disabled
    public void testAllWorkers() {
        Constants constants = constantsRepository.findAllConstants();
        subjectWorker.cacheRunImmutables(constants);
        enrolmentWorker.cacheRunImmutables(constants);
        programEncounterWorker.cacheRunImmutables(constants);

        var subjects = List.of("3f908d5b-d336-4604-896a-e7481bfe5972", "9197245a-541f-4d1b-be47-a96f8843e727");

        for (var s : subjects) {
            var subject = avniSubjectRepository.getSubject(s);
            subjectWorker.processSubject(subject, true);
            var enrolments = (List<String>) subject.get("enrolments");
            for (var enl : enrolments) {
                var enrolment = avniEnrolmentRepository.getEnrolment(enl);
                enrolmentWorker.processEnrolment(enrolment, true);
                var encounters = (List<String>) enrolment.get("encounters");
                for (var encounterUuid : encounters) {
                    var programEncounter = programEncounterRepository.getProgramEncounter(encounterUuid);
                    programEncounterWorker.processProgramEncounter(programEncounter, true);
                }
            }
        }
    }

    @Test
    @Disabled
    public void processProgramEncounter() {
        Constants constants = constantsRepository.findAllConstants();
        programEncounterWorker.cacheRunImmutables(constants);

        var programEncounter = programEncounterRepository.getProgramEncounter("c9add1fd-0be6-49db-a4a3-181e49f82a30");
        programEncounterWorker.processProgramEncounter(programEncounter, true);

    }

    /**
     * Debug test for ANC Clinic Visit sync (Avni -> Bahmni)
     *
     * ANC Clinic Visit is a PROGRAM ENCOUNTER in Avni, synced to Bahmni as a Visit.
     *
     * Sync Direction: Avni (ProgramEncounter) -> Bahmni (Visit/Encounter)
     * Worker: ProgramEncounterWorker
     * Mappings: ProgramEncounter group with CommunityProgramEncounter_* types
     */
    @Test
    @org.junit.jupiter.api.Tag("external")
    public void debugANCClinicVisitSync() {
        System.out.println("\n========== DEBUG: ANC Clinic Visit Sync (Avni -> Bahmni) ==========\n");
        System.out.println("This syncs ProgramEncounters from Avni to Visits in Bahmni");

        Constants constants = constantsRepository.findAllConstants();
        programEncounterWorker.cacheRunImmutables(constants);

        // Search for subject with Patient Identifier = GAN279732
        String patientId = "GAN279732";
        System.out.println("\nSTEP 1: Find subject in Avni with Patient Identifier: " + patientId);

        HashMap<String, Object> concepts = new HashMap<>();
        concepts.put("Patient Identifier", patientId);

        Subject[] subjects = avniSubjectRepository.getSubjects("Individual", concepts);
        System.out.println("  Found " + subjects.length + " subjects with Patient Identifier = " + patientId);

        if (subjects.length > 0) {
            Subject subject = subjects[0];
            System.out.println("  Subject UUID: " + subject.getUuid());
            System.out.println("  Subject Name: " + subject.getFirstName() + " " + subject.getLastName());
            System.out.println("  Subject Registration Date: " + subject.getRegistrationDate());

            // Get enrolments for this subject
            System.out.println("\nSTEP 2: Find enrolments and program encounters");
            @SuppressWarnings("unchecked")
            List<String> enrolments = (List<String>) subject.get("enrolments");
            System.out.println("  DEBUG: Raw enrolments list: " + enrolments);
            if (enrolments != null && !enrolments.isEmpty()) {
                System.out.println("  Subject has " + enrolments.size() + " enrolments");
                for (String enrolmentUuid : enrolments) {
                    var enrolment = avniEnrolmentRepository.getEnrolment(enrolmentUuid);
                    if (enrolment != null) {
                        System.out.println("  - Enrolment Program: " + enrolment.getProgram() + " (UUID: " + enrolmentUuid + ")");
                        System.out.println("    Enrolment Date: " + enrolment.getEnrolmentDateTime());

                        @SuppressWarnings("unchecked")
                        List<String> encounterUuids = (List<String>) enrolment.get("encounters");
                        if (encounterUuids != null && !encounterUuids.isEmpty()) {
                            System.out.println("    Has " + encounterUuids.size() + " program encounters");
                            for (String encUuid : encounterUuids) {
                                ProgramEncounter programEncounter = programEncounterRepository.getProgramEncounter(encUuid);
                                if (programEncounter != null) {
                                    System.out.println("    - Encounter Type: " + programEncounter.getEncounterType() + " (UUID: " + encUuid + ")");
                                    if ("ANC Clinic Visit".equals(programEncounter.getEncounterType())) {
                                        System.out.println("\n    ANC Clinic Visit found: " + encUuid);
                                        Object encounterDateObj = programEncounter.get("Encounter date time");
                                        System.out.println("      Encounter Date value: " + encounterDateObj + " (type: " + (encounterDateObj == null ? "NULL" : encounterDateObj.getClass().getSimpleName()) + ")");
                                        System.out.println("      Scheduled Date: " + programEncounter.getEarliestScheduledDate());
                                        System.out.println("      Is Completed: " + programEncounter.isCompleted());
                                        System.out.println("      *** ALL FIELDS: " + programEncounter.getProperties().keySet());

                                        if (programEncounter.isCompleted()) {
                                            System.out.println("\nSTEP 3: Processing COMPLETED ANC Clinic Visit: " + encUuid);
                                            try {
                                                programEncounterWorker.processProgramEncounter(programEncounter, false);
                                                System.out.println("  Sync attempted - check Bahmni for visit");
                                            } catch (org.avni_integration_service.bahmni.client.WebClientsException e) {
                                                System.out.println("  ERROR during sync: " + e.getClass().getSimpleName());
                                                System.out.println("    HTTP Status Code: " + e.getStatusCode());
                                                System.out.println("    Message: " + e.getMessage());
                                                e.printStackTrace();
                                            } catch (Exception e) {
                                                System.out.println("  ERROR during sync: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                                                e.printStackTrace();
                                            }
                                            System.out.println("\n========== Sync completed ==========\n");
                                            return;
                                        } else {
                                            System.out.println("      (Skipping - scheduled but not completed)");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                System.out.println("  Subject has no enrolments");
            }
        }

        // Also search through all program encounters from 2025
        System.out.println("\n--- Also searching all program encounters from 2025 ---\n");
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(2025, 0, 1, 0, 0, 0);
        java.util.Date lastModified = cal.getTime();
        int maxPages = 10;
        int pageNum = 0;
        java.util.Set<String> encounterTypes = new java.util.HashSet<>();

        while (pageNum < maxPages) {
            ProgramEncountersResponse response = programEncounterRepository.getProgramEncounters(lastModified);
            ProgramEncounter[] allEncounters = response.getContent();
            System.out.println("Page " + (pageNum + 1) + ": Found " + allEncounters.length + " program encounters");

            if (allEncounters.length == 0) break;

            for (ProgramEncounter encounter : allEncounters) {
                String encounterType = encounter.getEncounterType();
                encounterTypes.add(encounterType);

                if ("ANC Clinic Visit".equals(encounterType)) {
                    System.out.println("\nProcessing ANC Clinic Visit (ProgramEncounter): " + encounter.getUuid());
                    System.out.println("  Subject ID: " + encounter.getSubjectId());
                    System.out.println("  Encounter Date: " + encounter.getEncounterDateTime());
                    System.out.println("  Is Completed: " + encounter.isCompleted());

                    if (!encounter.isCompleted()) {
                        System.out.println("\n  *** WARNING: Encounter has NO DATE - will be FILTERED/SKIPPED! ***");
                        System.out.println("  *** Set 'Encounter date time' in Avni to enable sync ***\n");
                    }

                    programEncounterWorker.processProgramEncounter(encounter, false);

                    if (encounter.isCompleted()) {
                        System.out.println("  Sync attempted - check Bahmni for visit");
                    } else {
                        System.out.println("  Skipped (incomplete encounter)");
                    }
                    System.out.println("\n========== Sync completed ==========\n");
                    return;
                }

                // Update lastModified to get next page
                if (encounter.getLastModifiedDateTime() != null) {
                    lastModified = java.sql.Timestamp.valueOf(encounter.getLastModifiedDateTime());
                }
            }

            pageNum++;
            if (response.getTotalPages() <= 1) break;
        }

        System.out.println("\nAll program encounter types found: " + encounterTypes);
        System.out.println("No ANC Clinic Visit program encounters found in " + pageNum + " pages");
        System.out.println("\nNOTE: Create an ANC Clinic Visit program encounter in Avni for patient " + patientId);
        System.out.println("\n========== Sync completed ==========\n");
    }

    @Test
    public void syncSpecificEncounterByUUID() {
        System.out.println("\n========== DEBUG: Direct Sync of Specific Encounter by UUID ==========\n");

        String encounterUuid = "dd2b2e77-f2e5-4390-8e00-dfa01a0fcca1";
        System.out.println("Fetching and syncing encounter: " + encounterUuid);

        Constants constants = constantsRepository.findAllConstants();
        programEncounterWorker.cacheRunImmutables(constants);

        ProgramEncounter encounter = programEncounterRepository.getProgramEncounter(encounterUuid);

        if (encounter != null) {
            System.out.println("\n✓ Encounter found!");
            System.out.println("  Encounter Type: " + encounter.getEncounterType());
            System.out.println("  Subject ID: " + encounter.getSubjectId());

            // Get the raw "Encounter date time" value
            Object encounterDateObj = encounter.get("Encounter date time");
            System.out.println("\n[RAW FIELD DATA]");
            System.out.println("  Raw 'Encounter date time' value: " + encounterDateObj);
            System.out.println("  Type: " + (encounterDateObj == null ? "NULL" : encounterDateObj.getClass().getSimpleName()));

            // Now test parsing
            System.out.println("\n[PARSING TEST]");
            System.out.println("  getEncounterDateTime(): " + encounter.getEncounterDateTime());
            System.out.println("  isCompleted(): " + encounter.isCompleted());

            if (encounter.isCompleted()) {
                System.out.println("\n[SYNC ATTEMPT]");
                try {
                    programEncounterWorker.processProgramEncounter(encounter, false);
                    System.out.println("✓ Sync successful! Check Bahmni for the visit.");
                } catch (Exception e) {
                    System.out.println("✗ Sync failed:");
                    System.out.println("  Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("\n✗ Encounter is NOT completed (no encounter date)");
            }
        } else {
            System.out.println("✗ Encounter NOT found in Avni");
        }

        System.out.println("\n========== Test completed ==========\n");
    }

    @Test
    @org.junit.jupiter.api.Tag("external")
    public void debugANCClinicVisitSyncMultiplePatients() {
        System.out.println("\n========== DEBUG: ANC Clinic Visit Sync - Testing Multiple Patients ==========\n");
        System.out.println("This searches through all ANC Clinic Visit encounters and tests sync with the first completed one\n");

        Constants constants = constantsRepository.findAllConstants();
        programEncounterWorker.cacheRunImmutables(constants);

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(2025, 0, 1, 0, 0, 0);
        java.util.Date lastModified = cal.getTime();
        int pageNum = 0;
        int maxPages = 50;  // Search through more pages to find good data
        int completedEncounterCount = 0;
        int totalEncounterCount = 0;

        while (pageNum < maxPages) {
            ProgramEncountersResponse response = programEncounterRepository.getProgramEncounters(lastModified);
            ProgramEncounter[] allEncounters = response.getContent();
            System.out.println("Page " + (pageNum + 1) + ": Found " + allEncounters.length + " program encounters");

            if (allEncounters.length == 0) break;

            for (ProgramEncounter encounter : allEncounters) {
                if ("ANC Clinic Visit".equals(encounter.getEncounterType())) {
                    totalEncounterCount++;
                    System.out.println("\n[" + totalEncounterCount + "] ANC Clinic Visit found");
                    System.out.println("  Subject ID: " + encounter.getSubjectId());
                    System.out.println("  Encounter UUID: " + encounter.getUuid());
                    System.out.println("  Encounter Date: " + encounter.getEncounterDateTime());
                    System.out.println("  Is Completed: " + encounter.isCompleted());

                    if (encounter.isCompleted()) {
                        completedEncounterCount++;
                        System.out.println("\n✓ FOUND COMPLETED ENCOUNTER! Syncing now...\n");

                        try {
                            programEncounterWorker.processProgramEncounter(encounter, false);
                            System.out.println("✓ Sync successful - check Bahmni for visit");
                        } catch (org.avni_integration_service.bahmni.client.WebClientsException e) {
                            System.out.println("✗ ERROR during sync (HTTP " + e.getStatusCode() + "): " + e.getMessage());
                        } catch (Exception e) {
                            System.out.println("✗ ERROR during sync: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                        }
                        System.out.println("\n========== Sync completed ==========\n");
                        return;
                    }
                }

                // Update lastModified to get next page
                if (encounter.getLastModifiedDateTime() != null) {
                    lastModified = java.sql.Timestamp.valueOf(encounter.getLastModifiedDateTime());
                }
            }

            pageNum++;
            if (response.getTotalPages() <= 1) break;
        }

        System.out.println("\n========== SUMMARY ==========");
        System.out.println("Total ANC Clinic Visit encounters found: " + totalEncounterCount);
        System.out.println("Completed encounters (ready for sync): " + completedEncounterCount);
        System.out.println("Pages searched: " + pageNum);
        System.out.println("\nNo completed ANC Clinic Visit encounters found in " + pageNum + " pages");
        System.out.println("========== Sync completed ==========\n");
    }
}
