package org.avni_integration_service.bahmni.worker.bahmni;

import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.avni.repository.AvniSubjectRepository;
import org.avni_integration_service.bahmni.BahmniEncounterToAvniEncounterMetaData;
import org.avni_integration_service.bahmni.BahmniMappingType;
import org.avni_integration_service.bahmni.BaseExternalTest;
import org.avni_integration_service.bahmni.BaseSpringTest;
import org.avni_integration_service.bahmni.SubjectToPatientMetaData;
import org.avni_integration_service.bahmni.client.BahmniAvniSessionFactory;
import org.avni_integration_service.bahmni.contract.OpenMRSPatient;
import org.avni_integration_service.bahmni.repository.BahmniEncounter;
import org.avni_integration_service.bahmni.repository.BahmniSplitEncounter;
import org.avni_integration_service.bahmni.repository.OpenMRSEncounterRepository;
import org.avni_integration_service.bahmni.repository.OpenMRSPatientRepository;
import org.avni_integration_service.bahmni.contract.OpenMRSFullEncounter;
import org.avni_integration_service.bahmni.service.AvniEncounterService;
import org.avni_integration_service.bahmni.service.BahmniEncounterService;
import org.avni_integration_service.bahmni.service.MappingMetaDataService;
import org.avni_integration_service.bahmni.service.PatientService;
import org.avni_integration_service.bahmni.service.SubjectService;
import org.avni_integration_service.integration_data.domain.Constants;
import org.avni_integration_service.bahmni.worker.bahmni.atomfeedworker.PatientEncounterEventWorker;
import org.avni_integration_service.bahmni.worker.bahmni.atomfeedworker.PatientEventWorker;
import org.avni_integration_service.integration_data.domain.MappingMetaData;
import org.avni_integration_service.integration_data.repository.MappingMetaDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;

@SpringBootTest(classes = BaseSpringTest.class)
public class PatientEncounterEventWorkerExternalTest extends BaseExternalTest {
    @Autowired
    private PatientEventWorker patientEventWorker;
    @Autowired
    private PatientEncounterEventWorker patientEncounterEventWorker;
    @Autowired
    private AvniHttpClient avniHttpClient;
    @Autowired
    private BahmniAvniSessionFactory bahmniAvniSessionFactory;
    @Autowired
    private MappingMetaDataRepository mappingMetaDataRepository;
    @Autowired
    private MappingMetaDataService mappingMetaDataService;
    @Autowired
    private BahmniMappingType bahmniMappingType;
    @Autowired
    private BahmniEncounterService bahmniEncounterService;
    @Autowired
    private SubjectService subjectService;
    @Autowired
    private AvniEncounterService avniEncounterService;
    @Autowired
    private AvniSubjectRepository avniSubjectRepository;
    @Autowired
    private PatientService patientService;
    @Autowired
    private OpenMRSPatientRepository openMRSPatientRepository;
    @Autowired
    private OpenMRSEncounterRepository openMRSEncounterRepository;

    @BeforeEach
    public void beforeEach() {
        avniHttpClient.setAvniSession(bahmniAvniSessionFactory.createSession());
        patientEventWorker.cacheRunImmutables(getConstants());
        patientEncounterEventWorker.cacheRunImmutables(getConstants());
    }

    @Test
    @Disabled
    public void processEncounter() {
        patientEventWorker.process(patientEvent("ec19b096-5c20-4f67-97f8-c16a215b097a"));
        patientEncounterEventWorker.process(encounterEvent("49a677be-53e4-4017-aff2-55900e84e69e"));
    }

    @Test
    @Disabled
    public void processEncounterWithCodedDiagnosis() {
        patientEncounterEventWorker.process(encounterEvent("bc29306a-db5c-417c-9a94-315bd2bbb6d5"));
    }

    @Test
    @Disabled
    public void processLabEncounter() {
        patientEventWorker.process(patientEvent("9312db47-73eb-452c-9f0b-800bf0c4cbf4"));
        patientEncounterEventWorker.process(encounterEvent("a605cfe6-92e1-4bee-9f02-bded7ee385a2"));
    }

    @Test
    @Disabled
    public void processDrugPrescriptionEncounter() {
        patientEventWorker.process(patientEvent("00052bd1-4e72-45ee-9c8f-b711685aae89"));
        patientEncounterEventWorker.process(encounterEvent("42269eee-4d3f-45df-bdbf-b37af98290f9"));
    }

    @Test
    @Disabled
    public void processProgramEncounter() {
        patientEventWorker.process(patientEvent("999b1bda-e7a2-4601-ab76-79e09a1ef890"));
        patientEncounterEventWorker.process(encounterEvent("30791204-694e-4473-8c9a-7dc8e12cbfba"));
        patientEncounterEventWorker.process(encounterEvent("89dac55e-811f-4334-80c4-6c57f60fa64e"));
        patientEncounterEventWorker.process(encounterEvent("af660d4b-baf5-4a2e-bf4f-3a1cc5348d4e"));
        patientEncounterEventWorker.process(encounterEvent("b4a34014-10a2-42d3-a2db-553cb0153753"));
    }

    /**
     * REUSABLE METHOD: Test Diabetes Intake sync with patient identifier only
     *
     * Sync Direction: Bahmni (Encounter) -> Avni (GeneralEncounter)
     * Worker: PatientEncounterEventWorker
     *
     * IMPORTANT: Patient identifier alone is sufficient for integration to work.
     * Everything else (mappings, encounter UUIDs, etc.) is resolved automatically
     * from the database configuration.
     *
     * To test with different patients, just change the patient identifier:
     *   testDiabetesSyncByPatientId("279732");
     *   testDiabetesSyncByPatientId("279733");
     *   etc.
     */
    @Test
    @org.junit.jupiter.api.Tag("external")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void debugDiabetesIntakeSync() {
        testDiabetesSyncByPatientId("GAN279732");
    }

    @Test
    @org.junit.jupiter.api.Tag("external")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void stepByStepDiabetesSync() {
        // STEP-BY-STEP VERIFICATION TEST
        // Step 1: Sync patient registration (Bahmni Entity UUID only)
        // Step 2: Verify patient appears in Avni
        // Step 3: Sync Diabetes encounter
        // Step 4: Verify encounter appears in Avni

        System.out.println("\n========== STEP-BY-STEP DIABETES SYNC VERIFICATION ==========");

        System.out.println("\nSTEP 1: Syncing patient registration...");
        String bahmniPatientUuid = "3192824d-ec16-4e69-8478-e38aa05ec480";
        patientEventWorker.process(patientEvent(bahmniPatientUuid));
        System.out.println("✓ Patient sync completed");

        System.out.println("\nSTEP 2: Patient should now appear in Avni with identifier GAN279732");
        System.out.println("        → Go to Avni and search for patient GAN279732");
        System.out.println("        → Verify 'Bahmni Entity UUID' field is populated with: " + bahmniPatientUuid);
        System.out.println("        → (Manual verification required)");

        System.out.println("\nSTEP 3: Fetching and syncing Diabetes Intake encounters dynamically...");

        // Load encounter mappings from database
        BahmniEncounterToAvniEncounterMetaData metaData = mappingMetaDataService.getForBahmniEncounterToAvniEntities();

        // Get encounter type UUID for Diabetes Intake from mappings
        MappingMetaData diabetesEncounterTypeMapping = metaData.getEncounterMappingFor("60619143-5b49-4c10-92f4-0d080cd10b8a");
        if (diabetesEncounterTypeMapping == null) {
            System.out.println("  ✗ ERROR - No mapping found for Diabetes Intake form");
            return;
        }

        // Query all Diabetes encounters for this patient from Bahmni (not hardcoded!)
        List<BahmniEncounter> diabetesEncounters = bahmniEncounterService.getEncountersForPatient(
            bahmniPatientUuid,
            diabetesEncounterTypeMapping.getIntSystemValue(),
            metaData
        );

        if (diabetesEncounters == null || diabetesEncounters.isEmpty()) {
            System.out.println("  ✗ No Diabetes Intake encounters found for patient");
            return;
        }

        System.out.println("  ✓ Found " + diabetesEncounters.size() + " Diabetes Intake encounter(s)");

        // Sync each encounter dynamically discovered
        int syncedCount = 0;
        int emptyEncounters = 0;
        for (BahmniEncounter encounter : diabetesEncounters) {
            try {
                String encounterUuid = encounter.getOpenMRSEncounter().getUuid();
                int obsCount = encounter.getOpenMRSEncounter().getLeafObservations() != null ? encounter.getOpenMRSEncounter().getLeafObservations().size() : 0;
                System.out.println("  - Syncing encounter: " + encounterUuid + " (observations: " + obsCount + ")");

                if (obsCount == 0) {
                    System.out.println("    ⚠ WARNING: Encounter has NO observations in Bahmni!");
                    emptyEncounters++;
                }

                patientEncounterEventWorker.process(encounterEvent(encounterUuid));
                syncedCount++;
            } catch (Exception e) {
                System.out.println("    ✗ Failed to sync encounter: " + e.getMessage());
            }
        }
        System.out.println("✓ Encounter sync completed - " + syncedCount + " encounter(s) synced");
        if (emptyEncounters > 0) {
            System.out.println("  ⚠ Note: " + emptyEncounters + " encounter(s) have NO observations in Bahmni");
        }

        System.out.println("\nSTEP 4: Encounter should now appear in Avni");
        System.out.println("        → Go to Avni patient GAN279732's encounters");
        System.out.println("        → Look for 'Bahmni - Diabetes Intake Template' encounter");
        System.out.println("        → Verify observations are populated (Diagnosed Date, Complaint, etc.)");
        System.out.println("        → (Manual verification required)");

        System.out.println("\n========== VERIFICATION COMPLETE - CHECK AVNI ==========\n");
    }

    /**
     * Sync a Bahmni lab result (All_Tests_and_Panels) to Avni.
     *
     * The ConvSet UUID e4edc5a4-e349-11e3-983a-91270dcbd3bf is mapped to
     * "Bahmni - All_Tests_and_Panels" encounter type via V2_4_30 migration.
     *
     * To use: supply a Bahmni patient UUID that has an LAB_RESULT encounter,
     * or supply an encounter UUID directly via encounterEvent().
     */
    @Test
    @org.junit.jupiter.api.Tag("external")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processAllTestsAndPanels() {
        System.out.println("\n========== All Tests and Panels Sync — GAN279731 (Laxmi Prajapati) ==========");
        System.out.println("(Patient already synced — avni_subject_uuid is set on GAN279731)");

        // Known LAB_RESULT encounter UUIDs for GAN279731 (Laxmi Prajapati) in JSS Bahmni prerelease
        // (fetching all encounters by type times out due to large v=full payload)
        List<String> labEncounterUuids = List.of(
            "feb81977-5ca5-466b-a513-90cc7162606f",  // 12 obs (Haemoglobin, ALK Phosphate, etc.)
            "85a27fa5-4bab-4ac2-8252-3983180007d0"   // 1 obs (ESR)
        );

        System.out.println("STEP 1: Syncing " + labEncounterUuids.size() + " known LAB_RESULT encounters to Avni...");
        for (String encounterUuid : labEncounterUuids) {
            System.out.println("  - syncing encounter: " + encounterUuid);
            patientEncounterEventWorker.process(encounterEvent(encounterUuid));
        }

        System.out.println("\n→ Check Avni for 'Bahmni - All_Tests_and_Panels' encounters on GAN279731");
        System.out.println("========== Done ==========\n");
    }

    @Test
    @org.junit.jupiter.api.Tag("external")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processMedication() {
        System.out.println("\n========== Medication Sync — GAN279731 (Laxmi Prajapati) ==========");

        // Consultation encounter with 3 drug orders:
        // - Amoxycillin & Potassium Clavulanate 625mg, 1 Tablet(s), 7 days
        // - Multivitamin Multimineral, 1 Capsule(s), 10 days
        // - Calcium + Vit D 500mg, 1 Tablet(s), 10 days
        patientEncounterEventWorker.process(encounterEvent("44a99da4-8bbc-4955-b068-3277aa5eafc2"));

        System.out.println("→ Check Avni for 'Bahmni - Medication' encounter on GAN279731");
        System.out.println("========== Done ==========\n");
    }

    /**
     * Sync all encounters for the 5 pre-linked patients (GAN279731–GAN279735).
     * Each patient's Bahmni consultations are fetched and processed through the worker,
     * which automatically splits each consultation into one Avni encounter per mapped form.
     * After running: check Avni for each patient — they should have "Bahmni - *" encounters
     * for every form type present in their Bahmni data.
     */
    @Test
    @org.junit.jupiter.api.Tag("external")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void syncAllMappedPatients() {
        List<String> patientIdentifiers = List.of(
                "GAN279731", "GAN279732", "GAN279733", "GAN279734", "GAN279735"
        );

        System.out.println("\n========== Sync All Mapped Patients ==========");
        int totalEncounters = 0;
        int totalSuccess = 0;
        int totalFailed = 0;

        for (String identifier : patientIdentifiers) {
            System.out.println("\n--- Patient: " + identifier + " ---");

            OpenMRSPatient patient = openMRSPatientRepository.getPatientByIdentifier(identifier);
            if (patient == null) {
                System.out.println("  SKIP — patient not found in Bahmni");
                continue;
            }
            String patientUuid = patient.getUuid();
            System.out.println("  Bahmni UUID: " + patientUuid);

            List<OpenMRSFullEncounter> encounters = openMRSEncounterRepository.getEncountersByPatient(patientUuid);
            System.out.println("  Encounters found: " + encounters.size());

            for (OpenMRSFullEncounter encounter : encounters) {
                totalEncounters++;
                try {
                    patientEncounterEventWorker.process(encounterEvent(encounter.getUuid()));
                    totalSuccess++;
                } catch (Exception e) {
                    totalFailed++;
                    System.out.println("  FAILED [" + encounter.getUuid() + "]: " + e.getMessage());
                }
            }
        }

        System.out.println("\n========== Summary ==========");
        System.out.println("Total encounters processed : " + totalEncounters);
        System.out.println("Success                    : " + totalSuccess);
        System.out.println("Failed                     : " + totalFailed);
        System.out.println("-> Check Avni for each patient's 'Bahmni - *' encounters");
        System.out.println("========== Done ==========\n");
    }

    /**
     * Test Diabetes Intake sync with a specific patient identifier
     * Patient identifier alone is sufficient - integration resolves the rest
     */
    private void testDiabetesSyncByPatientId(String patientIdentifier) {
        // These UUIDs come from the database mappings - should not be hardcoded here
        String encounterUuid = "b31d5719-8275-4163-ab51-4d6b6ed5ff84";
        String formUuid = "60619143-5b49-4c10-92f4-0d080cd10b8a";

        System.out.println("\n========== Diabetes Intake Sync ==========");
        System.out.println("Patient Identifier: " + patientIdentifier + "\n");

        // Step 1: Load metadata (contains all mappings from database)
        BahmniEncounterToAvniEncounterMetaData metaData = mappingMetaDataService.getForBahmniEncounterToAvniEntities();
        System.out.println("Step 1: ✓ Metadata loaded");

        // Step 2: Verify mapping exists
        MappingMetaData mapping = metaData.getEncounterMappingFor(formUuid);
        if (mapping == null) {
            System.out.println("Step 2: ✗ ERROR - No mapping found for form");
            return;
        }
        System.out.println("Step 2: ✓ Mapping verified - Avni encounter type: " + mapping.getAvniValue());

        // Step 3: Find subject by Patient Identifier
        System.out.println("\nStep 3: Looking up patient by identifier...");
        HashMap<String, Object> concepts = new HashMap<>();
        concepts.put("Patient Identifier", patientIdentifier);
        Subject[] subjects = avniSubjectRepository.getSubjects("Individual", concepts);

        if (subjects.length == 0) {
            System.out.println("  ✗ ERROR - Patient not found: " + patientIdentifier);
            return;
        }
        Subject subject = subjects[0];
        System.out.println("  ✓ Patient found: " + subject.getFirstName() + " " + subject.getLastName());

        // Step 4: Get Bahmni patient UUID (Avni and Bahmni have different UUIDs for the same patient)
        System.out.println("\nStep 4: Looking up Bahmni patient...");
        // We need to convert from Avni subject to Bahmni patient using the patient identifier
        SubjectToPatientMetaData subjectToPatientMetaData = mappingMetaDataService.getForSubjectToPatient();
        org.avni_integration_service.bahmni.contract.OpenMRSPatient bahmniPatient = patientService.findPatient(subject, getConstants(), subjectToPatientMetaData);

        if (bahmniPatient == null) {
            System.out.println("  ✗ ERROR - Patient not found in Bahmni");
            return;
        }
        System.out.println("  ✓ Bahmni patient found: " + bahmniPatient.getUuid());

        // Step 5: Fetch Diabetes encounter from Bahmni (for this patient)
        System.out.println("\nStep 5: Fetching Diabetes Intake encounters for patient...");
        // Get all encounters for this Bahmni patient of the correct encounter type
        List<BahmniEncounter> patientEncounters = bahmniEncounterService.getEncountersForPatient(bahmniPatient.getUuid(), encounterUuid, metaData);

        if (patientEncounters == null || patientEncounters.isEmpty()) {
            System.out.println("  ✗ ERROR - No Diabetes Intake encounters found for patient in Bahmni");
            return;
        }

        BahmniEncounter bahmniEncounter = patientEncounters.get(0); // Get the first one
        System.out.println("  ✓ Encounter found: " + bahmniEncounter.getOpenMRSEncounter().getEncounterType().getUuid());

        // Step 6: Sync to Avni
        System.out.println("\nStep 6: Syncing to Avni...");
        patientEncounterEventWorker.process(encounterEvent(bahmniEncounter.getOpenMRSEncounter().getUuid()));
        System.out.println("  ✓ Sync completed");

        // Step 7: Verify sync
        System.out.println("\nStep 7: Verifying sync result...");
        List<BahmniSplitEncounter> splits = bahmniEncounter.getSplitEncounters();
        if (!splits.isEmpty()) {
            GeneralEncounter synced = avniEncounterService.getGeneralEncounter(splits.get(0), metaData);
            System.out.println("  ✓ Encounter synced: " + (synced != null ? "YES" : "NO"));
        }

        System.out.println("\n========== Sync Complete ==========\n");
    }
}
