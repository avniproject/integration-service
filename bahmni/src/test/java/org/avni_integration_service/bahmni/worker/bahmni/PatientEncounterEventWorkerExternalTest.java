package org.avni_integration_service.bahmni.worker.bahmni;

import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.avni.repository.AvniSubjectRepository;
import org.avni_integration_service.bahmni.BahmniEncounterToAvniEncounterMetaData;
import org.avni_integration_service.bahmni.BahmniMappingType;
import org.avni_integration_service.bahmni.BaseExternalTest;
import org.avni_integration_service.bahmni.BaseSpringTest;
import org.avni_integration_service.bahmni.client.BahmniAvniSessionFactory;
import org.avni_integration_service.bahmni.repository.BahmniEncounter;
import org.avni_integration_service.bahmni.repository.BahmniSplitEncounter;
import org.avni_integration_service.bahmni.service.AvniEncounterService;
import org.avni_integration_service.bahmni.service.BahmniEncounterService;
import org.avni_integration_service.bahmni.service.MappingMetaDataService;
import org.avni_integration_service.bahmni.service.SubjectService;
import org.avni_integration_service.bahmni.worker.bahmni.atomfeedworker.PatientEncounterEventWorker;
import org.avni_integration_service.bahmni.worker.bahmni.atomfeedworker.PatientEventWorker;
import org.avni_integration_service.integration_data.domain.MappingMetaData;
import org.avni_integration_service.integration_data.repository.MappingMetaDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.transaction.Transactional;
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
     * FIXED: Debug test for Diabetes Intake sync using Patient Identifier lookup
     *
     * Sync Direction: Bahmni (Encounter) -> Avni (GeneralEncounter)
     * Worker: PatientEncounterEventWorker
     * Patient: 279731 (Patient Identifier in Avni)
     * Approach: Find by Patient Identifier first
     */
    @Test
    @org.junit.jupiter.api.Tag("external")
    @Transactional
    public void debugDiabetesIntakeSyncFixed() {
        String patientId = "279731";  // Patient Identifier without GAN prefix
        String encounterUuid = "b31d5719-8275-4163-ab51-4d6b6ed5ff84";
        String formUuid = "60619143-5b49-4c10-92f4-0d080cd10b8a";

        System.out.println("\n========== DEBUG: Diabetes Intake Sync (FIXED) ==========\n");

        // Step 1: Find subject in Avni using Patient Identifier (like ANC sync)
        System.out.println("STEP 1: Find subject in Avni using Patient Identifier");
        System.out.println("  - Searching for Patient Identifier: " + patientId);
        
        HashMap<String, Object> concepts = new HashMap<>();
        concepts.put("Patient Identifier", patientId);
        
        Subject[] subjects = avniSubjectRepository.getSubjects("Individual", concepts);
        System.out.println("  - Found " + subjects.length + " subjects with Patient Identifier = " + patientId);
        
        if (subjects.length == 0) {
            System.out.println("  - ERROR: No subject found with Patient Identifier = " + patientId);
            return;
        }
        
        Subject subject = subjects[0];
        System.out.println("  - Subject UUID: " + subject.getUuid());
        System.out.println("  - Subject Name: " + subject.getFirstName() + " " + subject.getLastName());

        // Step 1.5: Create registration encounter first (critical for linking)
        System.out.println("\nSTEP 1.5: Create registration encounter in Avni");
        System.out.println("  - Processing patient event to create registration encounter");
        
        // Find the Bahmni patient UUID for this patient
        // For GAN279732, we need to find the corresponding Bahmni patient UUID
        String bahmniPatientUuid = "1da4aa76-97f3-4656-8bb1-fe6b3d748cb0"; // This might be the same as GAN279731, need to verify for GAN279732
        
        // Create registration encounter first
        patientEventWorker.process(patientEvent(bahmniPatientUuid));
        System.out.println("  - Registration encounter created");
        
        // Step 2: Load metadata
        BahmniEncounterToAvniEncounterMetaData metaData = mappingMetaDataService.getForBahmniEncounterToAvniEntities();
        System.out.println("\nSTEP 2: Metadata loaded");
        System.out.println("  - BahmniEntityUuidConcept: " + metaData.getBahmniEntityUuidConcept());

        // Step 3: Check if mapping exists
        boolean hasMapping = metaData.hasBahmniConceptSet(formUuid);
        MappingMetaData mapping = metaData.getEncounterMappingFor(formUuid);
        System.out.println("\nSTEP 3: Check mapping for form " + formUuid);
        System.out.println("  - Has mapping: " + hasMapping);
        if (mapping != null) {
            System.out.println("  - Avni encounter type: " + mapping.getAvniValue());
            System.out.println("  - Mapping group: " + mapping.getMappingGroup().getName());
        } else {
            System.out.println("  - ERROR: No mapping found!");
            return;
        }

        // Step 4: Find registration encounter in Avni (using subject UUID)
        System.out.println("\nSTEP 4: Find registration encounter in Avni");
        System.out.println("  - Using subject UUID: " + subject.getUuid());
        // Note: This step might need adjustment based on how registration encounters are stored
        
        // Step 4: Fetch encounter from Bahmni
        System.out.println("\nSTEP 4: Fetch encounter from Bahmni");
        BahmniEncounter bahmniEncounter = bahmniEncounterService.getEncounter(encounterUuid, metaData);
        if (bahmniEncounter == null) {
            System.out.println("  - ERROR: Could not fetch encounter from Bahmni!");
            return;
        }
        System.out.println("  - Fetched successfully");
        System.out.println("  - Encounter type: " + bahmniEncounter.getEncounterTypeUuid());

        // Step 5: Get split encounters
        List<BahmniSplitEncounter> splits = bahmniEncounter.getSplitEncounters();
        System.out.println("\nSTEP 6: Split encounters");
        System.out.println("  - Count: " + splits.size());
        for (BahmniSplitEncounter split : splits) {
            System.out.println("  - Form: " + split.getFormConceptSetUuid());
        }

        // Step 6: Actually run sync
        System.out.println("\nSTEP 6: Running actual sync...");
        patientEncounterEventWorker.process(encounterEvent(encounterUuid));
        System.out.println("  - Sync completed");

        // Step 7: Verify
        System.out.println("\nSTEP 7: Verify - check Avni for encounter");
        if (!splits.isEmpty()) {
            GeneralEncounter created = avniEncounterService.getGeneralEncounter(splits.get(0), metaData);
            System.out.println("  - Encounter now exists: " + (created != null));
        }

        System.out.println("\n========== END DEBUG (FIXED) ==========\n");
    }

    /**
     * Debug test for Diabetes Intake sync
     * Patient: GAN279731 (Bahmni UUID: 1da4aa76-97f3-4656-8bb1-fe6b3d748cb0)
     * Encounter: Diabetes Intake Template (UUID: b31d5719-8275-4163-ab51-4d6b6ed5ff84)
     */
    @Test
    @org.junit.jupiter.api.Tag("external")
    @Transactional
    public void debugDiabetesIntakeSync() {
        String patientUuid = "1da4aa76-97f3-4656-8bb1-fe6b3d748cb0";
        String encounterUuid = "b31d5719-8275-4163-ab51-4d6b6ed5ff84";
        String formUuid = "60619143-5b49-4c10-92f4-0d080cd10b8a";

        System.out.println("\n========== DEBUG: Diabetes Intake Sync ==========\n");

        // Step 1: Load metadata
        BahmniEncounterToAvniEncounterMetaData metaData = mappingMetaDataService.getForBahmniEncounterToAvniEntities();
        System.out.println("STEP 1: Metadata loaded");
        System.out.println("  - BahmniEntityUuidConcept: " + metaData.getBahmniEntityUuidConcept());

        // Step 2: Check if mapping exists
        boolean hasMapping = metaData.hasBahmniConceptSet(formUuid);
        MappingMetaData mapping = metaData.getEncounterMappingFor(formUuid);
        System.out.println("\nSTEP 2: Check mapping for form " + formUuid);
        System.out.println("  - Has mapping: " + hasMapping);
        if (mapping != null) {
            System.out.println("  - Avni encounter type: " + mapping.getAvniValue());
            System.out.println("  - Mapping group: " + mapping.getMappingGroup().getName());
        } else {
            System.out.println("  - ERROR: No mapping found!");
            return;
        }

        // Step 3: Find registration encounter in Avni
        System.out.println("\nSTEP 3: Find registration encounter in Avni");
        System.out.println("  - Searching for Bahmni Entity UUID: " + patientUuid);
        GeneralEncounter avniPatient = subjectService.findPatient(metaData, patientUuid);
        System.out.println("  - Found: " + (avniPatient != null));
        if (avniPatient != null) {
            System.out.println("  - Subject ID: " + avniPatient.getSubjectId());
        } else {
            System.out.println("  - ERROR: No registration encounter found!");
            System.out.println("  - Make sure you created an encounter with 'Bahmni Entity UUID' = " + patientUuid);
            return;
        }

        // Step 4: Fetch encounter from Bahmni
        System.out.println("\nSTEP 4: Fetch encounter from Bahmni");
        BahmniEncounter bahmniEncounter = bahmniEncounterService.getEncounter(encounterUuid, metaData);
        if (bahmniEncounter == null) {
            System.out.println("  - ERROR: Could not fetch encounter from Bahmni!");
            return;
        }
        System.out.println("  - Fetched successfully");
        System.out.println("  - Encounter type: " + bahmniEncounter.getEncounterTypeUuid());

        // Step 5: Get split encounters
        List<BahmniSplitEncounter> splits = bahmniEncounter.getSplitEncounters();
        System.out.println("\nSTEP 5: Split encounters");
        System.out.println("  - Count: " + splits.size());
        for (BahmniSplitEncounter split : splits) {
            System.out.println("  - Form: " + split.getFormConceptSetUuid());
        }

        // Step 6: Check existing encounter
        if (!splits.isEmpty()) {
            BahmniSplitEncounter split = splits.get(0);
            System.out.println("\nSTEP 6: Check existing Avni encounter");
            GeneralEncounter existing = avniEncounterService.getGeneralEncounter(split, metaData);
            System.out.println("  - Exists: " + (existing != null));

            // Step 7: Determine action
            System.out.println("\nSTEP 7: Expected action");
            if (existing == null && avniPatient != null) {
                System.out.println("  - Should CREATE new encounter");
            } else if (existing != null && avniPatient != null) {
                System.out.println("  - Should UPDATE existing encounter");
            } else {
                System.out.println("  - No action (missing data)");
            }
        }

        // Step 8: Actually run the sync
        System.out.println("\nSTEP 8: Running actual sync...");
        patientEncounterEventWorker.process(encounterEvent(encounterUuid));
        System.out.println("  - Sync completed");

        // Step 9: Verify
        System.out.println("\nSTEP 9: Verify - check Avni for the encounter");
        if (!splits.isEmpty()) {
            GeneralEncounter created = avniEncounterService.getGeneralEncounter(splits.get(0), metaData);
            System.out.println("  - Encounter now exists: " + (created != null));
        }

        System.out.println("\n========== END DEBUG ==========\n");
    }
}
