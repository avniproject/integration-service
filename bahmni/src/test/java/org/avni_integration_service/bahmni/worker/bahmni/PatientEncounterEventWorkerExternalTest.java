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
import org.avni_integration_service.bahmni.repository.BahmniEncounter;
import org.avni_integration_service.bahmni.repository.BahmniSplitEncounter;
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
