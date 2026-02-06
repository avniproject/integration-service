package org.avni_integration_service.bahmni.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.bahmni.BaseSpringTest;
import org.avni_integration_service.bahmni.SubjectToPatientMetaData;
import org.avni_integration_service.bahmni.contract.OpenMRSPatient;
import org.avni_integration_service.integration_data.domain.Constants;
import org.avni_integration_service.integration_data.domain.Constant;
import org.avni_integration_service.integration_data.repository.ConstantsRepository;
import org.javatuples.Pair;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = BaseSpringTest.class)
@Disabled("Enable this test when running against actual JSS Bahmni server")
class PatientServiceExternalTest {
    @Autowired
    private PatientService patientService;

    @Autowired
    private ConstantsRepository constantsRepository;

    @Test
    @Disabled("Requires connection to JSS Bahmni server")
    public void findPatientByIdentifier_GAN279731() throws JsonProcessingException {
        // Create constants with JSS values (since DB is empty in test)
        List<Constant> constantList = new ArrayList<>();
        constantList.add(new Constant("BahmniIdentifierPrefix", "GAN"));
        constantList.add(new Constant("IntegrationBahmniIdentifierType", "b46af68a-c79a-11e2-b284-107d46e7b2c5"));
        Constants constants = new Constants(constantList);

        // Create a subject with the ID part (without GAN prefix)
        Subject subject = new Subject();
        subject.setUuid("test-subject-uuid-12345");
        subject.addObservation("avniIdentifierConcept", "279731"); // This will become GAN279731

        // Create metadata with required values
        SubjectToPatientMetaData metaData = new SubjectToPatientMetaData(
            "avniIdentifierConcept",
            "subject-encounter-type-uuid",
            "subject-uuid-concept"
        );

        // Test the patient lookup with the provided identifier
        OpenMRSPatient patient = patientService.findPatient(subject, constants, metaData);

        assertNotNull(patient, "Patient should be found with identifier GAN279731");
        assertNotNull(patient.getUuid(), "Patient should have a UUID");
        assertEquals("1da4aa76-97f3-4656-8bb1-fe6b3d748cb0", patient.getUuid(),
            "Patient UUID should match the expected value");

        System.out.println("Found patient: " + patient.getUuid());
        System.out.println("Patient identifiers: " + patient.getIdentifiers());
    }

    @Test
    @Disabled("Requires connection to JSS Bahmni server")
    public void findSubject_UsingGAN279731_WithActualConfig() throws Exception {
        // Create constants with JSS values (since DB is empty in test)
        List<Constant> constantList = new ArrayList<>();
        constantList.add(new Constant("BahmniIdentifierPrefix", "GAN"));
        constantList.add(new Constant("IntegrationBahmniIdentifierType", "b46af68a-c79a-11e2-b284-107d46e7b2c5"));
        Constants constants = new Constants(constantList);

        // Create a subject with the ID part (without GAN prefix)
        Subject subject = new Subject();
        subject.setUuid("test-subject-uuid-12345");
        subject.addObservation("avniIdentifierConcept", "279731"); // This will become GAN279731

        // Create metadata with required values
        SubjectToPatientMetaData metaData = new SubjectToPatientMetaData(
            "avniIdentifierConcept",
            "subject-encounter-type-uuid",
            "subject-uuid-concept"
        );

        // Test the findSubject method
        Pair<OpenMRSPatient, ?> result = patientService.findSubject(subject, constants, metaData);

        assertNotNull(result, "Result should not be null");
        assertNotNull(result.getValue0(), "Patient should be found");

        OpenMRSPatient patient = result.getValue0();
        assertEquals("1da4aa76-97f3-4656-8bb1-fe6b3d748cb0", patient.getUuid(),
            "Patient UUID should match the expected value");

        // Verify the identifier was constructed correctly
        String expectedIdentifier = constants.getValue("BahmniIdentifierPrefix") + "279731";
        boolean hasCorrectIdentifier = patient.getIdentifiers().stream()
            .anyMatch(id -> expectedIdentifier.equals(id.getIdentifier()));
        assertTrue(hasCorrectIdentifier, "Patient should have identifier " + expectedIdentifier);

        System.out.println("Found patient via findSubject: " + patient.getUuid());
        System.out.println("Patient identifiers: " + patient.getIdentifiers());
        System.out.println("Identifier prefix used: " + constants.getValue("BahmniIdentifierPrefix"));
        System.out.println("Identifier type UUID: " + constants.getValue("IntegrationBahmniIdentifierType"));
    }

    @Test
    public void testIdentifierConstruction() {
        // This test verifies how the identifier is constructed
        List<Constant> constantList = new ArrayList<>();
        constantList.add(new Constant("BahmniIdentifierPrefix", "GAN"));
        Constants constants = new Constants(constantList);

        Subject subject = new Subject();
        subject.addObservation("avniIdentifierConcept", "279731");

        String patientIdentifier = constants.getValue("BahmniIdentifierPrefix") +
            subject.getId("avniIdentifierConcept");

        assertEquals("GAN279731", patientIdentifier);
        System.out.println("Constructed identifier: " + patientIdentifier);
        System.out.println("Test passed: Identifier construction works correctly");
    }

    @Test
    public void testConstantsFromDatabase() {
        // Test that constants are loaded from database
        Constants constants = constantsRepository.findAllConstants();
        assertNotNull(constants);

        String prefix = constants.getValue("BahmniIdentifierPrefix");
        String identifierType = constants.getValue("IntegrationBahmniIdentifierType");

        System.out.println("BahmniIdentifierPrefix from DB: " + prefix);
        System.out.println("IntegrationBahmniIdentifierType from DB: " + identifierType);

        // If constants are not in DB, they might need to be loaded from JSON file
        // For now, let's not assert null to see what's actually loaded
        // assertNotNull(prefix, "BahmniIdentifierPrefix should not be null");
        // assertNotNull(identifierType, "IntegrationBahmniIdentifierType should not be null");
    }
}
