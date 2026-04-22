package org.avni_integration_service.bahmni.service;

import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.bahmni.BaseSpringTest;
import org.avni_integration_service.bahmni.contract.OpenMRSPatient;
import org.avni_integration_service.bahmni.contract.OpenMRSPersonAttribute;
import org.avni_integration_service.bahmni.repository.OpenMRSPatientRepository;
import org.avni_integration_service.integration_data.domain.Constant;
import org.avni_integration_service.integration_data.domain.Constants;
import org.avni_integration_service.integration_data.repository.ConstantsRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * External tests for Avni DEA Link write-back to Bahmni.
 *
 * Requires:
 *   1. Live JSS Bahmni server connection (bahmni-application.properties configured)
 *   2. "Avni DEA Link" Person Attribute Type created in Bahmni Admin
 *   3. DB constants: AvniBaseUrl and AvniDEALinkAttributeTypeUuid inserted into constants table
 *
 * Run tests in order: testSkipsWhenNotConfigured → testCreateDEALink → testUpdateDEALink
 */
@SpringBootTest(classes = BaseSpringTest.class)
@Disabled
class AvniDEALinkExternalTest {

    // Real JSS patient UUID (same patient used in PatientEventWorkerExternalTest)
    private static final String TEST_PATIENT_UUID = "42baa8d4-145d-4d76-b4de-6aad19cb3f2a";
    // Fake Avni subject UUID for testing
    private static final String TEST_SUBJECT_UUID = "test-avni-subject-uuid-for-dea-link";

    @Autowired
    private PatientService patientService;

    @Autowired
    private OpenMRSPatientRepository openMRSPatientRepository;

    @Autowired
    private ConstantsRepository constantsRepository;

    /**
     * Verifies that writeAvniDEALink silently skips when constants are not configured.
     * Run this BEFORE inserting DB constants.
     */
    @Test
    void testSkipsWhenNotConfigured() {
        Constants emptyConstants = new Constants(new ArrayList<>());
        Subject subject = subjectWithUuid(TEST_SUBJECT_UUID);
        OpenMRSPatient patient = openMRSPatientRepository.getPatient(TEST_PATIENT_UUID);
        assertNotNull(patient, "Patient should exist in Bahmni");

        // Should return silently without throwing
        assertDoesNotThrow(() -> patientService.writeAvniDEALink(subject, patient, emptyConstants));

        System.out.println("✓ writeAvniDEALink skipped silently when constants not configured");
    }

    /**
     * Verifies that the Avni DEA Link person attribute is CREATED in Bahmni
     * when the patient has no existing DEA link attribute.
     * Run AFTER inserting DB constants and BEFORE testUpdateDEALink.
     */
    @Test
    void testCreateDEALink() {
        Constants constants = constantsRepository.findAllConstants();
        assertConstantsConfigured(constants);

        Subject subject = subjectWithUuid(TEST_SUBJECT_UUID);
        OpenMRSPatient patient = openMRSPatientRepository.getPatient(TEST_PATIENT_UUID);
        assertNotNull(patient, "Patient should exist in Bahmni");

        String attributeTypeUuid = constants.getValue("AvniDEALinkAttributeTypeUuid");
        String avniBaseUrl = constants.getValue("AvniBaseUrl");
        String expectedUrl = avniBaseUrl + "#/app/subject?uuid=" + TEST_SUBJECT_UUID;

        patientService.writeAvniDEALink(subject, patient, constants);

        // Re-fetch patient to verify attribute was written
        OpenMRSPatient updatedPatient = openMRSPatientRepository.getPatient(TEST_PATIENT_UUID);
        OpenMRSPersonAttribute deaAttribute = findDEAAttribute(updatedPatient, attributeTypeUuid);

        assertNotNull(deaAttribute, "Avni DEA Link attribute should have been created in Bahmni");
        assertEquals(expectedUrl, deaAttribute.getValue().toString(),
                "DEA link URL should match expected Avni subject URL");

        System.out.println("✓ Avni DEA Link attribute created: " + deaAttribute.getValue());
    }

    /**
     * Verifies that the Avni DEA Link attribute is UPDATED (not duplicated)
     * when the patient already has an existing DEA link attribute.
     * Run AFTER testCreateDEALink.
     */
    @Test
    void testUpdateDEALink() {
        Constants constants = constantsRepository.findAllConstants();
        assertConstantsConfigured(constants);

        String attributeTypeUuid = constants.getValue("AvniDEALinkAttributeTypeUuid");

        // Use a different subject UUID to verify the value changes
        String updatedSubjectUuid = "updated-avni-subject-uuid-for-dea-link";
        Subject subject = subjectWithUuid(updatedSubjectUuid);
        OpenMRSPatient patient = openMRSPatientRepository.getPatient(TEST_PATIENT_UUID);
        assertNotNull(patient, "Patient should exist in Bahmni");

        long attributeCountBefore = patient.getPerson().getAttributes().stream()
                .filter(a -> attributeTypeUuid.equals(a.getAttributeType().getUuid()))
                .count();
        assertTrue(attributeCountBefore >= 1, "Patient should already have a DEA link attribute from testCreateDEALink");

        patientService.writeAvniDEALink(subject, patient, constants);

        // Re-fetch and verify exactly ONE attribute (no duplicate)
        OpenMRSPatient updatedPatient = openMRSPatientRepository.getPatient(TEST_PATIENT_UUID);
        long attributeCountAfter = updatedPatient.getPerson().getAttributes().stream()
                .filter(a -> attributeTypeUuid.equals(a.getAttributeType().getUuid()))
                .count();

        assertEquals(1, attributeCountAfter, "Should have exactly ONE DEA link attribute (no duplicates)");

        OpenMRSPersonAttribute deaAttribute = findDEAAttribute(updatedPatient, attributeTypeUuid);
        String avniBaseUrl = constants.getValue("AvniBaseUrl");
        String expectedUrl = avniBaseUrl + "#/app/subject?uuid=" + updatedSubjectUuid;
        assertEquals(expectedUrl, deaAttribute.getValue().toString(), "DEA link should reflect updated subject UUID");

        System.out.println("✓ Avni DEA Link attribute updated without duplication: " + deaAttribute.getValue());
    }

    private Subject subjectWithUuid(String uuid) {
        Subject subject = new Subject();
        subject.setUuid(uuid);
        return subject;
    }

    private OpenMRSPersonAttribute findDEAAttribute(OpenMRSPatient patient, String attributeTypeUuid) {
        return patient.getPerson().getAttributes().stream()
                .filter(a -> attributeTypeUuid.equals(a.getAttributeType().getUuid()))
                .findFirst().orElse(null);
    }

    private void assertConstantsConfigured(Constants constants) {
        assertNotNull(constants.getValue("AvniBaseUrl"),
                "AvniBaseUrl must be configured in constants table before running this test");
        assertNotNull(constants.getValue("AvniDEALinkAttributeTypeUuid"),
                "AvniDEALinkAttributeTypeUuid must be configured in constants table before running this test");
    }
}
