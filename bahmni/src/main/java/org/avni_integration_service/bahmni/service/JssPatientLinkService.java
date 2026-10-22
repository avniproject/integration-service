package org.avni_integration_service.bahmni.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.bahmni.ConstantKey;
import org.avni_integration_service.bahmni.Names;
import org.avni_integration_service.bahmni.contract.OpenMRSPatient;
import org.avni_integration_service.bahmni.contract.OpenMRSPersonAttribute;
import org.avni_integration_service.bahmni.repository.OpenMRSPatientRepository;
import org.avni_integration_service.bahmni.repository.openmrs.OpenMRSPersonRepository;
import org.avni_integration_service.integration_data.domain.Constants;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Links an Avni individual to an existing Bahmni patient by storing the Avni subject UUID on the patient
 * (person attribute AvniSubjectUuidBahmniAttributeTypeUuid). That attribute is the only link the visit sync uses.
 * Nothing is ever created here: no Bahmni patient, no Avni individual.
 */
@Service
public class JssPatientLinkService {
    private static final Logger logger = Logger.getLogger(JssPatientLinkService.class);

    public enum LinkStatus {
        LINKED, ALREADY_LINKED, PATIENT_NOT_FOUND, MULTIPLE_PATIENTS_FOUND, LINKED_TO_ANOTHER_PERSON
    }

    public record LinkResult(LinkStatus status, OpenMRSPatient patient, String otherAvniSubjectUuid) {
        public boolean isLinked() {
            return status == LinkStatus.LINKED || status == LinkStatus.ALREADY_LINKED;
        }
    }

    private final OpenMRSPatientRepository patientRepository;
    private final OpenMRSPersonRepository personRepository;

    public JssPatientLinkService(OpenMRSPatientRepository patientRepository, OpenMRSPersonRepository personRepository) {
        this.patientRepository = patientRepository;
        this.personRepository = personRepository;
    }

    public boolean isEnabled(Constants constants) {
        return attributeTypeUuid(constants) != null;
    }

    /** Bahmni ID from an "Individual ID Update" visit, or null when the form does not say "Yes" with an ID. */
    public static String bahmniIdToLink(GeneralEncounter idUpdate) {
        if (Boolean.TRUE.equals(idUpdate.getVoided())) return null;
        Object registered = idUpdate.getObservation(Names.IdUpdateRegisteredInBahmniConcept);
        Object bahmniId = idUpdate.getObservation(Names.IdUpdateBahmniIdConcept);
        if (!Names.IdUpdateRegisteredYes.equals(registered) || bahmniId == null) return null;
        String normalised = bahmniId.toString().trim().toUpperCase();
        return normalised.isEmpty() ? null : normalised;
    }

    /** Avni subject UUID stored on the Bahmni patient, or null when the patient is not linked. */
    public String linkedAvniSubjectUuid(OpenMRSPatient patient, Constants constants) {
        OpenMRSPersonAttribute attribute = findLinkAttribute(patient, constants);
        return attribute == null || attribute.getValue() == null ? null : attribute.getValue().toString();
    }

    public LinkResult link(String avniSubjectUuid, String bahmniId, Constants constants, boolean dryRun) {
        List<OpenMRSPatient> matches = patientsWithExactId(bahmniId);
        if (matches.isEmpty()) return new LinkResult(LinkStatus.PATIENT_NOT_FOUND, null, null);
        if (matches.size() > 1) return new LinkResult(LinkStatus.MULTIPLE_PATIENTS_FOUND, null, null);
        OpenMRSPatient patient = matches.get(0);

        String linkedTo = linkedAvniSubjectUuid(patient, constants);
        if (avniSubjectUuid.equals(linkedTo)) return new LinkResult(LinkStatus.ALREADY_LINKED, patient, null);
        if (linkedTo != null) return new LinkResult(LinkStatus.LINKED_TO_ANOTHER_PERSON, patient, linkedTo);

        if (!dryRun) {
            writeLink(patient, avniSubjectUuid, bahmniId, constants);
            logger.info(String.format("Linked Bahmni patient %s to Avni subject %s", bahmniId, avniSubjectUuid));
        }
        return new LinkResult(LinkStatus.LINKED, patient, null);
    }

    /** Removes the link from a patient, but only if it points to this Avni subject (used when a Bahmni ID is corrected). */
    public boolean unlink(String avniSubjectUuid, String bahmniId, Constants constants, boolean dryRun) {
        List<OpenMRSPatient> matches = patientsWithExactId(bahmniId);
        if (matches.size() != 1) return false;
        OpenMRSPatient patient = matches.get(0);
        OpenMRSPersonAttribute attribute = findLinkAttribute(patient, constants);
        if (attribute == null || !Objects.equals(avniSubjectUuid, String.valueOf(attribute.getValue()))) return false;
        if (!dryRun) {
            personRepository.removePersonAttribute(patient.getPerson().getUuid(), attribute.getUuid());
            logger.info(String.format("Unlinked Bahmni patient %s from Avni subject %s", bahmniId, avniSubjectUuid));
        }
        return true;
    }

    // JSS Bahmni can be slow to answer a write. A write that timed out may still have been saved, so check before writing again.
    private void writeLink(OpenMRSPatient patient, String avniSubjectUuid, String bahmniId, Constants constants) {
        for (int attempt = 1; ; attempt++) {
            try {
                personRepository.setPersonAttribute(patient.getPerson().getUuid(), attributeTypeUuid(constants), avniSubjectUuid, null);
                return;
            } catch (RuntimeException e) {
                if (!isTimeout(e)) throw e;
                logger.warn(String.format("Writing the Avni link to Bahmni patient %s timed out (attempt %d), checking whether it was saved", bahmniId, attempt));
                pause(5000L * attempt);
                List<OpenMRSPatient> current = patientsWithExactId(bahmniId);
                if (current.size() == 1 && avniSubjectUuid.equals(linkedAvniSubjectUuid(current.get(0), constants))) return;
                if (attempt >= 3) throw e;
            }
        }
    }

    private static boolean isTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause())
            if (t instanceof java.net.SocketTimeoutException || String.valueOf(t.getMessage()).contains("timed out")) return true;
        return false;
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Bahmni's identifier search also returns near matches (SEM5669 finds SEM05669 too); only the exact ID counts.
    // Leading zeros matter: SEM014770 and SEM14770 are different patients.
    private List<OpenMRSPatient> patientsWithExactId(String bahmniId) {
        return patientRepository.getPatientsByIdentifierPrefix(bahmniId).stream()
                .filter(p -> p != null && !p.isVoided() && p.getIdentifiers() != null
                        && p.getIdentifiers().stream().anyMatch(i -> bahmniId.equals(i.getIdentifier())))
                .collect(Collectors.toMap(OpenMRSPatient::getUuid, p -> p, (a, b) -> a, java.util.LinkedHashMap::new))
                .values().stream().collect(Collectors.toList());
    }

    private OpenMRSPersonAttribute findLinkAttribute(OpenMRSPatient patient, Constants constants) {
        String attributeTypeUuid = attributeTypeUuid(constants);
        if (attributeTypeUuid == null || patient.getPerson() == null || patient.getPerson().getAttributes() == null) return null;
        return patient.getPerson().getAttributes().stream()
                .filter(a -> a.getAttributeType() != null && attributeTypeUuid.equals(a.getAttributeType().getUuid()))
                .findFirst().orElse(null);
    }

    private static String attributeTypeUuid(Constants constants) {
        return constants.getValue(ConstantKey.AvniSubjectUuidBahmniAttributeTypeUuid.name());
    }
}
