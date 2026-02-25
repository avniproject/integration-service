package org.avni_integration_service.bahmni.mapper;

import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.bahmni.BahmniMappingGroup;
import org.avni_integration_service.bahmni.BahmniMappingType;
import org.avni_integration_service.bahmni.contract.OpenMRSPatient;
import org.avni_integration_service.bahmni.contract.OpenMRSPersonAttribute;
import org.avni_integration_service.bahmni.PatientToSubjectMetaData;
import org.avni_integration_service.integration_data.domain.MappingMetaData;
import org.avni_integration_service.bahmni.MappingMetaDataCollection;
import org.avni_integration_service.integration_data.repository.MappingMetaDataRepository;
import org.avni_integration_service.util.FormatAndParseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OpenMRSPatientMapper {
    private final MappingMetaDataRepository mappingMetaDataRepository;
    private final BahmniMappingGroup bahmniMappingGroup;
    private final BahmniMappingType bahmniMappingType;

    @Autowired
    public OpenMRSPatientMapper(MappingMetaDataRepository mappingMetaDataRepository, BahmniMappingGroup bahmniMappingGroup,
                                BahmniMappingType bahmniMappingType) {
        this.mappingMetaDataRepository = mappingMetaDataRepository;
        this.bahmniMappingGroup = bahmniMappingGroup;
        this.bahmniMappingType = bahmniMappingType;
    }

    public GeneralEncounter mapToAvniEncounter(OpenMRSPatient openMRSPatient, Subject subject, PatientToSubjectMetaData patientToSubjectMetaData, MappingMetaDataCollection conceptMetaData) {
        LinkedHashMap<String, Object> observations = mapToAvniObservations(openMRSPatient, patientToSubjectMetaData, conceptMetaData);

        GeneralEncounter encounterRequest = new GeneralEncounter();
        encounterRequest.setSubjectId(subject.getUuid());
        encounterRequest.setEncounterType(patientToSubjectMetaData.patientEncounterType());
        encounterRequest.setEncounterDateTime(FormatAndParseUtil.now());
        encounterRequest.setObservations(observations);
        encounterRequest.set("cancelObservations", new HashMap<>());
        return encounterRequest;
    }

    public LinkedHashMap<String, Object> mapToAvniObservations(OpenMRSPatient openMRSPatient, PatientToSubjectMetaData patientToSubjectMetaData, MappingMetaDataCollection conceptMetaData) {
        LinkedHashMap<String, Object> observations = new LinkedHashMap<>();

        // Per integration requirement: Only sync Bahmni Entity UUID in patient registration form
        // Do NOT sync patient attributes (gender, phone, etc.) - they are not mapped and not needed
        // Patient attributes should be managed separately in Avni, not synced from Bahmni

        observations.put(patientToSubjectMetaData.bahmniEntityUuidConcept(), openMRSPatient.getUuid());
        return observations;
    }
}
