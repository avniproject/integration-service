package org.bahmni_avni_integration.worker.avni;

import org.apache.log4j.Logger;
import org.bahmni_avni_integration.contract.avni.Subject;
import org.bahmni_avni_integration.contract.avni.SubjectsResponse;
import org.bahmni_avni_integration.contract.bahmni.OpenMRSFullEncounter;
import org.bahmni_avni_integration.contract.bahmni.OpenMRSPatient;
import org.bahmni_avni_integration.integration_data.domain.*;
import org.bahmni_avni_integration.integration_data.internal.SubjectToPatientMetaData;
import org.bahmni_avni_integration.integration_data.repository.AvniEntityStatusRepository;
import org.bahmni_avni_integration.integration_data.repository.avni.AvniIgnoredConceptsRepository;
import org.bahmni_avni_integration.integration_data.repository.avni.AvniSubjectRepository;
import org.bahmni_avni_integration.service.*;
import org.bahmni_avni_integration.worker.ErrorRecordWorker;
import org.javatuples.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SubjectWorker implements ErrorRecordWorker {
    @Autowired
    private AvniEntityStatusRepository avniEntityStatusRepository;
    @Autowired
    private MappingMetaDataService mappingMetaDataService;
    @Autowired
    private AvniSubjectRepository avniSubjectRepository;
    @Autowired
    private PatientService patientService;
    @Autowired
    private EntityStatusService entityStatusService;
    @Autowired
    private ErrorService errorService;
    @Autowired
    private AvniIgnoredConceptsRepository avniIgnoredConceptsRepository;
    @Autowired
    private SubjectService subjectService;

    private static final Logger logger = Logger.getLogger(SubjectWorker.class);
    private SubjectToPatientMetaData metaData;
    private Constants constants;

    public void processSubjects() {
        while (true) {
            AvniEntityStatus status = avniEntityStatusRepository.findByEntityType(AvniEntityType.Subject);
            SubjectsResponse response = avniSubjectRepository.getSubjects(status.getReadUpto(), constants.getValue(ConstantKey.IntegrationAvniSubjectType));
            Subject[] subjects = response.getContent();
            int totalElements = response.getTotalElements();
            int totalPages = response.getTotalPages();
            logger.info(String.format("Found %d subjects that are newer than %s", subjects.length, status.getReadUpto()));
            if (subjects.length == 0) break;
            for (Subject subject : subjects) {
                processSubject(subject);
            }
            if (totalElements == 1 && totalPages == 1) break;
        }
    }

    private void removeIgnoredObservations(Subject subject) {
        var observations = (LinkedHashMap<String, Object>) subject.get("observations");
        avniIgnoredConceptsRepository.getIgnoredConcepts().forEach(observations::remove);
        subject.set("observations", observations);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void processSubject(Subject subject) {
        logger.debug("Processing subject %s".formatted(subject.getUuid()));
        if (hasDuplicates(subject)) {
            if(!subject.getVoided()) {
                logger.error("Create multiple subjects found error for subject %s identifier %s".formatted(subject.getUuid(), subject.getId(metaData)));
                patientService.processMultipleSubjectsFound(subject);
            } else {
                logger.debug("Skip voided subject %s because of having non voided duplicates".formatted(subject.getUuid()));
            }
            return;
        };
        removeIgnoredObservations(subject);
        Pair<OpenMRSPatient, OpenMRSFullEncounter> patientEncounter = patientService.findSubject(subject, constants, metaData);
        var patient = patientEncounter.getValue0();
        var encounter = patientEncounter.getValue1();

        if (encounter != null && patient != null) {
            logger.debug(String.format("Updating existing encounter %s for subject %s", encounter.getUuid(), subject.getUuid()));
            patientService.updateSubject(encounter, patient, subject, metaData, constants);
        } else if (encounter != null && patient == null) {
            // product-roadmap-todo: openmrs doesn't support the ability to find encounter without providing the patient hence this condition will never be reached
            patientService.processPatientIdChanged(subject, metaData);
        } else if (encounter == null && patient != null) {
            logger.debug(String.format("Creating new encounter for subject %s", subject.getUuid()));
            patientService.createSubject(subject, patient, metaData, constants);
        } else if (encounter == null && patient == null) {
            logger.debug(String.format("Creating new patient and new encounter for subject %s", subject.getUuid()));
            patientService.createPatientAndSubject(subject, metaData, constants);
        }
        entityStatusService.saveEntityStatus(subject);
    }

    private boolean hasDuplicates(Subject subject) {
        Subject[] subjects = subjectService.findSubjects(subject, metaData, constants);
        int sizeOfNonVoidedOtherThanSelf = Arrays.stream(subjects)
                .filter(s -> !s.getUuid().equals(subject.getUuid()))
                .filter(s -> !s.getVoided())
                .collect(Collectors.toList())
                .size();
        boolean hasDuplicates = sizeOfNonVoidedOtherThanSelf > 0;
        logger.debug("Duplicate subjects found for subject %s identifier %s voided %s".formatted(subject.getUuid(),
                subject.getId(metaData),
                subject.getVoided()));
        return hasDuplicates;
    }

    @Override
    public void processError(String entityUuid) {
        Subject subject = avniSubjectRepository.getSubject(entityUuid);
        if (subject == null) {
            logger.warn(String.format("Subject has been deleted now: %s", entityUuid));
            errorService.errorOccurred(entityUuid, ErrorType.EntityIsDeleted, AvniEntityType.Subject);
            return;
        }

        processSubject(subject);
    }

    public void cacheRunImmutables(Constants constants) {
        this.constants = constants;
        metaData = mappingMetaDataService.getForSubjectToPatient();
    }
}