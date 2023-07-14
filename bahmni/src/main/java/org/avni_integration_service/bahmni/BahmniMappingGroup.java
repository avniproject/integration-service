package org.avni_integration_service.bahmni;

import org.apache.log4j.Logger;
import org.avni_integration_service.integration_data.domain.MappingGroup;
import org.avni_integration_service.integration_data.repository.MappingGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BahmniMappingGroup {
    private static final Logger logger = Logger.getLogger(BahmniMappingGroup.class);
    public final MappingGroup patientSubject;
    public final MappingGroup generalEncounter;
    public final MappingGroup programEnrolment;
    public final MappingGroup programEncounter;
    public final MappingGroup observation;
    public final MappingGroup common;

    @Autowired
    public BahmniMappingGroup(MappingGroupRepository mappingGroupRepository) {
        this.patientSubject = mappingGroupRepository.findByNameAndIsVoidedFalse("PatientSubject");
        this.generalEncounter = mappingGroupRepository.findByNameAndIsVoidedFalse("GeneralEncounter");
        this.programEnrolment = mappingGroupRepository.findByNameAndIsVoidedFalse("ProgramEnrolment");
        this.programEncounter = mappingGroupRepository.findByNameAndIsVoidedFalse("ProgramEncounter");
        this.observation = mappingGroupRepository.findByNameAndIsVoidedFalse("Observation");
        this.common = mappingGroupRepository.findByNameAndIsVoidedFalse("Common");
    }
}
