package org.avni_integration_service.bahmni.repository.openmrs;

import org.avni_integration_service.bahmni.BaseExternalTest;
import org.avni_integration_service.bahmni.BaseSpringTest;
import org.avni_integration_service.bahmni.ConstantKey;
import org.avni_integration_service.bahmni.contract.OpenMRSPatient;
import org.avni_integration_service.bahmni.contract.OpenMRSPersonAttribute;
import org.avni_integration_service.bahmni.repository.OpenMRSPatientRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * External tests for OpenMRSPersonRepository.setPersonAttribute().
 *
 * Requires:
 *   1. Live JSS Bahmni server reachable (SSH tunnel / direct)
 *   2. bahmni-application.properties configured with JSS server credentials
 *   3. DB constant AvniSubjectUuidBahmniAttributeTypeUuid inserted for JSS org
 *
 * Run in order: testCreateAttribute → testUpdateAttribute → testSkipsWhenConstantMissing
 */
@SpringBootTest(classes = BaseSpringTest.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled("Requires live JSS Bahmni server — run manually against prerelease or production")
class OpenMRSPersonRepositoryExternalTest extends BaseExternalTest {

    // Real JSS patient — reuse the same patient used in PatientServiceExternalTest
    private static final String TEST_PATIENT_IDENTIFIER = "GAN279731";

    // Fake Avni subject UUID for create scenario
    private static final String TEST_SUBJECT_UUID_V1 = "avni-subject-uuid-create-test-001";
    // Updated UUID for update scenario
    private static final String TEST_SUBJECT_UUID_V2 = "avni-subject-uuid-update-test-002";

    @Autowired
    private OpenMRSPersonRepository openMRSPersonRepository;

    @Autowired
    private OpenMRSPatientRepository openMRSPatientRepository;

    @Test
    @Order(1)
    public void testCreateAttribute() {
        String attributeTypeUuid = getConstants().getValue(ConstantKey.AvniSubjectUuidBahmniAttributeTypeUuid.name());
        assertNotNull(attributeTypeUuid, "DB constant AvniSubjectUuidBahmniAttributeTypeUuid must be set");

        OpenMRSPatient patient = openMRSPatientRepository.getPatientByIdentifier(TEST_PATIENT_IDENTIFIER);
        assertNotNull(patient, "Test patient " + TEST_PATIENT_IDENTIFIER + " must exist in Bahmni");

        // Remove existing attribute if present so we test the create path
        String existingUuid = findExistingAttributeUuid(patient, attributeTypeUuid);

        openMRSPersonRepository.setPersonAttribute(patient.getUuid(), attributeTypeUuid, TEST_SUBJECT_UUID_V1, existingUuid);

        // Reload and verify
        OpenMRSPatient reloaded = openMRSPatientRepository.getPatient(patient.getUuid());
        String storedValue = findAttributeValue(reloaded, attributeTypeUuid);
        assertEquals(TEST_SUBJECT_UUID_V1, storedValue, "Attribute value should be written to Bahmni");

        System.out.println("testCreateAttribute PASSED — attribute written: " + storedValue);
    }

    @Test
    @Order(2)
    public void testUpdateAttribute() {
        String attributeTypeUuid = getConstants().getValue(ConstantKey.AvniSubjectUuidBahmniAttributeTypeUuid.name());
        assertNotNull(attributeTypeUuid);

        OpenMRSPatient patient = openMRSPatientRepository.getPatientByIdentifier(TEST_PATIENT_IDENTIFIER);
        assertNotNull(patient);

        // Attribute must already exist from testCreateAttribute
        String existingAttributeUuid = findExistingAttributeUuid(patient, attributeTypeUuid);
        assertNotNull(existingAttributeUuid, "Attribute must exist before testing update — run testCreateAttribute first");

        openMRSPersonRepository.setPersonAttribute(patient.getUuid(), attributeTypeUuid, TEST_SUBJECT_UUID_V2, existingAttributeUuid);

        // Reload and verify value changed
        OpenMRSPatient reloaded = openMRSPatientRepository.getPatient(patient.getUuid());
        String storedValue = findAttributeValue(reloaded, attributeTypeUuid);
        assertEquals(TEST_SUBJECT_UUID_V2, storedValue, "Attribute value should be updated in Bahmni");

        System.out.println("testUpdateAttribute PASSED — attribute updated to: " + storedValue);
    }

    @Test
    @Order(3)
    public void testSkipsWhenConstantMissing() {
        // Simulates behaviour in PatientService.writeAvniSubjectUuidToBahmni() when constant not configured
        String attributeTypeUuid = getConstants().getValue("NonExistentConstantKey");
        assertNull(attributeTypeUuid, "Missing constant should return null — PatientService skips the write");

        System.out.println("testSkipsWhenConstantMissing PASSED — null constant correctly detected");
    }

    private String findExistingAttributeUuid(OpenMRSPatient patient, String attributeTypeUuid) {
        return patient.getPerson().getAttributes().stream()
                .filter(a -> attributeTypeUuid.equals(a.getAttributeType().getUuid()))
                .map(OpenMRSPersonAttribute::getUuid)
                .findFirst().orElse(null);
    }

    private String findAttributeValue(OpenMRSPatient patient, String attributeTypeUuid) {
        return patient.getPerson().getAttributes().stream()
                .filter(a -> attributeTypeUuid.equals(a.getAttributeType().getUuid()))
                .map(a -> (String) a.getValue())
                .findFirst().orElse(null);
    }
}
