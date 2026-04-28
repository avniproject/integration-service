package org.avni_integration_service.bahmni.worker.avni;

import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.avni.repository.AvniSubjectRepository;
import org.avni_integration_service.bahmni.BaseExternalTest;
import org.avni_integration_service.bahmni.BaseSpringTest;
import org.avni_integration_service.bahmni.ConstantKey;
import org.avni_integration_service.bahmni.SubjectToPatientMetaData;
import org.avni_integration_service.bahmni.client.BahmniAvniSessionFactory;
import org.avni_integration_service.bahmni.contract.OpenMRSPatient;
import org.avni_integration_service.bahmni.repository.OpenMRSPatientRepository;
import org.avni_integration_service.bahmni.service.MappingMetaDataService;
import org.avni_integration_service.integration_data.domain.Constants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end test for Phase 1: Avni subject UUID written to Bahmni person attribute.
 *
 * Iterates over all known JSS identifier suffixes, syncs each Avni subject to Bahmni,
 * and verifies the person attribute is correctly written for every patient.
 *
 * Requires live JSS Bahmni and Avni prerelease servers.
 */
@SpringBootTest(classes = BaseSpringTest.class)
class SubjectWorkerAvniUuidSyncExternalTest extends BaseExternalTest {

    // Known JSS identifier suffixes (without prefix) that exist as Avni subjects
    private static final String[] KNOWN_IDENTIFIER_SUFFIXES = {"279731", "279732", "279733", "279734", "279735"};

    @Autowired
    private SubjectWorker subjectWorker;

    @Autowired
    private AvniHttpClient avniHttpClient;

    @Autowired
    private BahmniAvniSessionFactory bahmniAvniSessionFactory;

    @Autowired
    private AvniSubjectRepository avniSubjectRepository;

    @Autowired
    private OpenMRSPatientRepository openMRSPatientRepository;

    @Autowired
    private MappingMetaDataService mappingMetaDataService;

    @BeforeEach
    public void setUp() {
        avniHttpClient.setAvniSession(bahmniAvniSessionFactory.createSession());
        subjectWorker.cacheRunImmutables(getConstants());
    }

    @Test
    public void syncWritesAvniSubjectUuidToBahmniPersonAttribute() {
        Constants constants = getConstants();
        SubjectToPatientMetaData metaData = mappingMetaDataService.getForSubjectToPatient();

        String attributeTypeUuid = constants.getValue(ConstantKey.AvniSubjectUuidBahmniAttributeTypeUuid.name());
        assertNotNull(attributeTypeUuid, "DB constant AvniSubjectUuidBahmniAttributeTypeUuid must be set");

        String identifierPrefix = constants.getValue(ConstantKey.BahmniIdentifierPrefix.name());
        assertNotNull(identifierPrefix, "BahmniIdentifierPrefix must be configured");

        String subjectType = constants.getValue(ConstantKey.IntegrationAvniSubjectType.name());
        assertNotNull(subjectType, "IntegrationAvniSubjectType must be configured");

        String identifierConcept = metaData.avniIdentifierConcept();

        System.out.println("\n=== syncWritesAvniSubjectUuidToBahmniPersonAttribute (all patients) ===");

        int synced = 0;
        int skipped = 0;

        for (String suffix : KNOWN_IDENTIFIER_SUFFIXES) {
            System.out.println("\n--- Processing " + identifierPrefix + suffix + " ---");

            HashMap<String, Object> searchConcepts = new HashMap<>();
            searchConcepts.put(identifierConcept, suffix);
            Subject[] matches = avniSubjectRepository.getSubjects(subjectType, searchConcepts);
            if (matches.length == 0) {
                System.out.println("  Avni subject not found — skipping");
                skipped++;
                continue;
            }
            Subject avniSubject = matches[0];

            String fullIdentifier = identifierPrefix + suffix;
            OpenMRSPatient bahmniPatient = openMRSPatientRepository.getPatientByIdentifier(fullIdentifier);
            if (bahmniPatient == null) {
                System.out.println("  Bahmni patient not found — skipping");
                skipped++;
                continue;
            }
            System.out.println("  Avni: " + avniSubject.getUuid() + "  Bahmni uuid: " + bahmniPatient.getUuid());

            subjectWorker.processSubject(avniSubject, false);

            OpenMRSPatient reloaded = openMRSPatientRepository.getPatient(bahmniPatient.getUuid());
            assertNotNull(reloaded, "Bahmni patient " + fullIdentifier + " must still exist after sync");

            String writtenUuid = reloaded.getPerson().getAttributes().stream()
                    .filter(a -> attributeTypeUuid.equals(a.getAttributeType().getUuid()))
                    .map(a -> (String) a.getValue())
                    .findFirst().orElse(null);

            assertEquals(avniSubject.getUuid(), writtenUuid,
                    "Bahmni person attribute for " + fullIdentifier + " must equal Avni subject UUID");

            System.out.println("  PASSED — UUID written: " + writtenUuid);
            synced++;
        }

        System.out.println("\n=== Summary: " + synced + " synced, " + skipped + " skipped ===\n");
        assertTrue(synced > 0, "At least one patient must have been successfully synced");
    }
}
