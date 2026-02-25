package org.avni_integration_service.bahmni.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.ProgramEncounter;
import org.avni_integration_service.avni.repository.AvniEnrolmentRepository;
import org.avni_integration_service.bahmni.BahmniErrorType;
import org.avni_integration_service.bahmni.contract.OpenMRSFullEncounter;
import org.avni_integration_service.bahmni.contract.OpenMRSPatient;
import org.avni_integration_service.bahmni.mapper.avni.EncounterMapper;
import org.avni_integration_service.bahmni.repository.OpenMRSEncounterRepository;
import org.avni_integration_service.bahmni.repository.intmapping.MappingService;
import org.avni_integration_service.integration_data.domain.Constants;
import org.springframework.stereotype.Service;

@Service
public class ProgramEncounterService extends BaseAvniEncounterService {
    private final VisitService visitService;
    private final EncounterMapper encounterMapper;
    private final AvniBahmniErrorService avniBahmniErrorService;
    private final AvniEnrolmentRepository avniEnrolmentRepository;
    private static final Logger logger = Logger.getLogger(ProgramEncounterService.class);

    public ProgramEncounterService(PatientService patientService,
                                   MappingService mappingService,
                                   OpenMRSEncounterRepository openMRSEncounterRepository,
                                   VisitService visitService,
                                   EncounterMapper encounterMapper,
                                   AvniBahmniErrorService avniBahmniErrorService,
                                   AvniEnrolmentRepository avniEnrolmentRepository) {
        super(patientService, mappingService, openMRSEncounterRepository);
        this.visitService = visitService;
        this.encounterMapper = encounterMapper;
        this.avniBahmniErrorService = avniBahmniErrorService;
        this.avniEnrolmentRepository = avniEnrolmentRepository;
    }

    public OpenMRSFullEncounter createCommunityEncounter(ProgramEncounter programEncounter, OpenMRSPatient patient, Constants constants) {
        if (programEncounter.getVoided()) {
            logger.debug(String.format("Skipping voided Avni encounter %s", programEncounter.getUuid()));
            return null;
        }

        System.out.println("\n========== SUBSTEP 3A: FETCH ENROLMENT FROM AVNI ==========");
        System.out.println("Enrolment ID to fetch: " + programEncounter.getEnrolmentId());
        logger.debug(String.format("Creating new Bahmni Encounter for Avni encounter %s", programEncounter.getUuid()));
        var enrolment = avniEnrolmentRepository.getEnrolment(programEncounter.getEnrolmentId());

        if (enrolment == null) {
            System.out.println("✗ ERROR: Enrolment not found!");
            logger.error(String.format("Enrolment %s not found for encounter %s", programEncounter.getEnrolmentId(), programEncounter.getUuid()));
            return null;
        }

        System.out.println("✓ Enrolment Found");
        System.out.println("  - UUID: " + enrolment.getUuid());
        System.out.println("  - Program: " + enrolment.getProgram());
        System.out.println("  - Enrolment Date: " + enrolment.getEnrolmentDateTime());

        System.out.println("\n========== SUBSTEP 3B: GET OR CREATE VISIT IN BAHMNI ==========");
        System.out.println("Patient UUID: " + patient.getUuid());
        System.out.println("Program: " + enrolment.getProgram());
        System.out.println("Encounter Date for Visit: " + programEncounter.getEncounterDateTime());
        var visit = visitService.getOrCreateVisit(patient, enrolment, programEncounter.getEncounterDateTime());

        if (visit == null) {
            System.out.println("✗ ERROR: Visit creation failed!");
            logger.error(String.format("Failed to create visit for patient %s and enrolment %s", patient.getUuid(), enrolment.getUuid()));
            return null;
        }

        System.out.println("✓ Visit Created/Retrieved");
        System.out.println("  - Visit UUID: " + visit.getUuid());
        System.out.println("  - Visit Type UUID: " + (visit.getVisitType() != null ? visit.getVisitType().getUuid() : "NULL"));
        System.out.println("  - Start Date: " + visit.getStartDatetime());

        System.out.println("\n========== SUBSTEP 3C: MAP ENCOUNTER FROM AVNI TO BAHMNI FORMAT ==========");
        System.out.println("Encounter Type: " + programEncounter.getEncounterType());
        var encounter = encounterMapper.mapEncounter(programEncounter, patient.getUuid(), constants, visit);
        System.out.println("✓ Encounter mapped");
        System.out.println("  - Bahmni Encounter Type UUID: " + encounter.getEncounterType());
        System.out.println("  - Observations: will be posted to Bahmni");

        System.out.println("\n========== SUBSTEP 3D: CREATE ENCOUNTER IN BAHMNI ==========");
        var savedEncounter = openMRSEncounterRepository.createEncounter(encounter);

        if (savedEncounter != null) {
            System.out.println("✓ Successfully created Bahmni encounter");
            System.out.println("  - Encounter UUID: " + savedEncounter.getUuid());
        } else {
            System.out.println("✗ ERROR: Encounter creation returned null");
        }

        avniBahmniErrorService.successfullyProcessed(programEncounter);
        return savedEncounter;
    }

    public boolean shouldFilterEncounter(ProgramEncounter programEncounter) {
        return !programEncounter.isCompleted();
    }

    public void processPatientNotFound(ProgramEncounter programEncounter) {
        avniBahmniErrorService.errorOccurred(programEncounter, BahmniErrorType.NoPatientWithId);
    }

    public void updateCommunityEncounter(OpenMRSFullEncounter existingEncounter, ProgramEncounter programEncounter, Constants constants) {
        if (programEncounter.getVoided()) {
            logger.debug(String.format("Voiding Bahmni Encounter %s because the Avni encounter %s is voided",
                    existingEncounter.getUuid(),
                    programEncounter.getUuid()));
            openMRSEncounterRepository.voidEncounter(existingEncounter);
        } else {
            logger.debug(String.format("Updating existing Bahmni Encounter %s", existingEncounter.getUuid()));
            var openMRSEncounter = encounterMapper.mapEncounterToExistingEncounter(existingEncounter,
                    programEncounter,
                    constants);
            openMRSEncounterRepository.updateEncounter(openMRSEncounter);
            avniBahmniErrorService.successfullyProcessed(programEncounter);
        }
    }
}
