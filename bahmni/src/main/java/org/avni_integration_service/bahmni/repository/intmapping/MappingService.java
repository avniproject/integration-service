package org.avni_integration_service.bahmni.repository.intmapping;

import org.avni_integration_service.bahmni.BahmniMappingGroup;
import org.avni_integration_service.bahmni.BahmniMappingType;
import org.avni_integration_service.bahmni.MappingMetaDataCollection;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.MappingGroup;
import org.avni_integration_service.integration_data.domain.MappingMetaData;
import org.avni_integration_service.integration_data.domain.MappingType;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.avni_integration_service.integration_data.repository.MappingMetaDataRepository;
import org.avni_integration_service.util.ObsDataType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MappingService {
    private final MappingMetaDataRepository mappingMetaDataRepository;
    private final BahmniMappingGroup bahmniMappingGroup;
    private final BahmniMappingType bahmniMappingType;
    private final IntegrationSystemRepository integrationSystemRepository;

    @Autowired
    public MappingService(MappingMetaDataRepository mappingMetaDataRepository, BahmniMappingGroup bahmniMappingGroup,
                          BahmniMappingType bahmniMappingType, IntegrationSystemRepository integrationSystemRepository) {
        this.mappingMetaDataRepository = mappingMetaDataRepository;
        this.bahmniMappingGroup = bahmniMappingGroup;
        this.bahmniMappingType = bahmniMappingType;
        this.integrationSystemRepository = integrationSystemRepository;
    }

    public MappingMetaDataCollection findAll(MappingGroup mappingGroup, List<MappingType> mappingTypes) {
        return new MappingMetaDataCollection(mappingMetaDataRepository.findAllByMappingGroupAndMappingTypeInAndIsVoidedFalse(mappingGroup, mappingTypes));
    }

    public MappingMetaDataCollection findAll(MappingGroup mappingGroup, MappingType mappingType) {
        return new MappingMetaDataCollection(mappingMetaDataRepository.findAllByMappingGroupAndMappingTypeAndIsVoidedFalse(mappingGroup, mappingType));
    }

    public String getAvniValue(MappingGroup mappingGroup, MappingType mappingType) {
        MappingMetaData mapping = mappingMetaDataRepository.findByMappingGroupAndMappingTypeAndIsVoidedFalse(mappingGroup, mappingType);
        return getAvniValue(mapping);
    }

    private String getAvniValue(MappingMetaData mapping) {
        if (mapping == null) return null;
        return mapping.getAvniValue();
    }

    public String getBahmniValue(MappingGroup mappingGroup, MappingType mappingType) {
        MappingMetaData mapping = mappingMetaDataRepository.findByMappingGroupAndMappingTypeAndIsVoidedFalse(mappingGroup, mappingType);
        if (mapping == null) return null;
        return mapping.getIntSystemValue();
    }

    public String getBahmniValue(MappingGroup mappingGroup, MappingType mappingType, String avniValue) {
        MappingMetaData mapping = mappingMetaDataRepository.findByMappingGroupAndMappingTypeAndAvniValueAndIsVoidedFalse(mappingGroup, mappingType, avniValue);
        if (mapping == null) return null;
        return mapping.getIntSystemValue();
    }

    public MappingMetaData saveMapping(MappingGroup mappingGroup, MappingType mappingType, String bahmniValue, String avniValue, ObsDataType obsDataType) {
        MappingMetaData mappingMetaData = createMappingMetaData(mappingGroup, mappingType, bahmniValue, avniValue);
        mappingMetaData.setDataTypeHint(obsDataType);
        return mappingMetaDataRepository.save(mappingMetaData);
    }

    private MappingMetaData createMappingMetaData(MappingGroup mappingGroup, MappingType mappingType, String bahmniValue, String avniValue) {
        MappingMetaData mappingMetaData = new MappingMetaData();
        mappingMetaData.setMappingGroup(mappingGroup);
        mappingMetaData.setMappingType(mappingType);
        mappingMetaData.setIntSystemValue(bahmniValue);
        mappingMetaData.setAvniValue(avniValue);
        mappingMetaData.setIntegrationSystem(integrationSystemRepository.findBySystemType(IntegrationSystem.IntegrationSystemType.bahmni));
        return mappingMetaData;
    }

    public MappingMetaData saveMapping(MappingGroup mappingGroup, MappingType mappingType, String bahmniValue, String avniValue) {
        MappingMetaData mappingMetaData = createMappingMetaData(mappingGroup, mappingType, bahmniValue, avniValue);
        return mappingMetaDataRepository.save(mappingMetaData);
    }

    public String getBahmniValueForAvniIdConcept() {
        return getBahmniValue(bahmniMappingGroup.common, bahmniMappingType.avniUUIDConcept);
    }

    public String getBahmniFormUuidForProgramEncounter(String encounterType) {
        return getBahmniValue(bahmniMappingGroup.programEncounter,
                bahmniMappingType.communityProgramEncounterBahmniForm,
                encounterType);
    }

    public String getBahmniFormUuidForGeneralEncounter(String encounterType) {
        return getBahmniValue(bahmniMappingGroup.generalEncounter,
                bahmniMappingType.communityEncounterBahmniForm,
                encounterType);
    }

    public MappingMetaData findByMappingGroupAndMappingType(MappingGroup patientSubject, MappingType mappingType) {
        return mappingMetaDataRepository.findByMappingGroupAndMappingTypeAndIsVoidedFalse(patientSubject, mappingType);
    }
}
