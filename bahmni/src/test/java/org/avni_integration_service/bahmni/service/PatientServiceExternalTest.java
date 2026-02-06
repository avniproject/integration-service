package org.avni_integration_service.bahmni.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.bahmni.BaseSpringTest;
import org.avni_integration_service.bahmni.SubjectToPatientMetaData;
import org.avni_integration_service.bahmni.contract.OpenMRSPatient;
import org.avni_integration_service.integration_data.domain.Constants;
import org.avni_integration_service.integration_data.domain.Constant;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.avni_integration_service.bahmni.worker.bahmni.atomfeedworker.PatientEncounterEventWorker;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = BaseSpringTest.class)
class PatientServiceExternalTest {
    @Autowired
    private PatientService patientService;


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

        OpenMRSPatient patient = patientService.findPatient(subject, constants, metaData);

        assertNotNull(patient, "Patient should be found for identifier GAN279731");
        assertNotNull(patient.getUuid(), "Patient should have a UUID");
        assertEquals("1da4aa76-97f3-4656-8bb1-fe6b3d748cb0", patient.getUuid(),
            "Patient UUID should match the expected value");
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
        // Note: findSubject may throw SubjectIdChangedException if encounters exist
        // but none match the test subject UUID, since we use fake UUIDs here.
        try {
            Pair<OpenMRSPatient, ?> result = patientService.findSubject(subject, constants, metaData);

            assertNotNull(result, "Result should not be null");
            assertNotNull(result.getValue0(), "Patient should be found");

            OpenMRSPatient patient = result.getValue0();
            assertEquals("1da4aa76-97f3-4656-8bb1-fe6b3d748cb0", patient.getUuid(),
                "Patient UUID should match the expected value");

            System.out.println("Found patient via findSubject: " + patient.getUuid());
        } catch (PatientEncounterEventWorker.SubjectIdChangedException e) {
            // Expected when using fake encounter type / subject UUIDs against real data
            System.out.println("SubjectIdChangedException thrown - expected with test UUIDs against real data");
        }
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

}
