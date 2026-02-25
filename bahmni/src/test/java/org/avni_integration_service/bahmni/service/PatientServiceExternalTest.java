package org.avni_integration_service.bahmni.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.bahmni.BaseSpringTest;
import org.avni_integration_service.bahmni.SubjectToPatientMetaData;
import org.avni_integration_service.bahmni.contract.OpenMRSPatient;
import org.avni_integration_service.integration_data.domain.Constants;
import org.avni_integration_service.integration_data.domain.Constant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = BaseSpringTest.class)
class PatientServiceExternalTest {
    @Autowired
    private PatientService patientService;

    /**
     * REUSABLE METHOD: Test patient lookup by identifier(s)
     *
     * Can be used to test:
     * - Single patient lookup by ID
     * - Multiple patient lookups
     * - Patient lookup verification
     *
     * Default: Tests fetching multiple patients from Bahmni by their IDs
     * Usage: Can pass different patient IDs for testing different scenarios
     */
    @Test
    public void testPatientLookupByIdentifier() throws JsonProcessingException {
        // Test patient IDs to lookup
        String[] patientIds = {"279731", "279732", "279733", "279734", "279735", "279736"};

        // Setup constants with JSS Ganiyari values
        List<Constant> constantList = new ArrayList<>();
        constantList.add(new Constant("BahmniIdentifierPrefix", "GAN"));
        constantList.add(new Constant("IntegrationBahmniIdentifierType", "b46af68a-c79a-11e2-b284-107d46e7b2c5"));
        Constants constants = new Constants(constantList);

        // Setup metadata for patient lookup
        SubjectToPatientMetaData metaData = new SubjectToPatientMetaData(
            "avniIdentifierConcept",
            "subject-encounter-type-uuid",
            "subject-uuid-concept"
        );

        System.out.println("\n========================================");
        System.out.println("TESTING PATIENT LOOKUP BY IDENTIFIER");
        System.out.println("========================================\n");

        int found = 0;
        for (String id : patientIds) {
            // Create subject with patient ID
            Subject subject = new Subject();
            subject.setUuid("test-subject-uuid-" + id);
            subject.addObservation("avniIdentifierConcept", id);

            // Lookup patient in Bahmni
            OpenMRSPatient patient = patientService.findPatient(subject, constants, metaData);

            System.out.println("Lookup: GAN" + id);
            if (patient != null) {
                found++;
                System.out.println("  ✓ Status: FOUND");
                System.out.println("  UUID: " + patient.getUuid());
                System.out.println("  ID: " + patient.getPatientId());
                System.out.println("  Name: " + patient.getName());
                if (patient.getPerson() != null) {
                    System.out.println("  Gender: " + patient.getPerson().getGender());
                    System.out.println("  DOB: " + patient.getPerson().getBirthdate());
                }
            } else {
                System.out.println("  ✗ Status: NOT FOUND");
            }
            System.out.println();
        }

        // Summary
        System.out.println("========================================");
        System.out.println("SUMMARY");
        System.out.println("Found: " + found + " of " + patientIds.length + " patients");
        System.out.println("Success Rate: " + (found * 100 / patientIds.length) + "%");
        System.out.println("========================================\n");

        // Assertion: At least one patient should be found
        assertTrue(found > 0, "At least one patient should be found for testing");
    }

}
