package org.avni_integration_service.service;

import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.MappingMetaData;
import org.avni_integration_service.integration_data.repository.MappingGroupRepository;
import org.avni_integration_service.integration_data.repository.MappingMetaDataRepository;
import org.avni_integration_service.integration_data.repository.MappingTypeRepository;
import org.avni_integration_service.web.contract.MappingMetadataContract;
import org.springframework.stereotype.Service;

@Service
public class MappingMetadataService {
    private final MappingMetaDataRepository mappingMetaDataRepository;
    private final MappingGroupRepository mappingGroupRepository;
    private final MappingTypeRepository mappingTypeRepository;

    public MappingMetadataService(MappingMetaDataRepository mappingMetaDataRepository, MappingGroupRepository mappingGroupRepository, MappingTypeRepository mappingTypeRepository) {
        this.mappingMetaDataRepository = mappingMetaDataRepository;
        this.mappingGroupRepository = mappingGroupRepository;
        this.mappingTypeRepository = mappingTypeRepository;
    }

    public void createOrUpdateMappingMetadata(MappingMetadataContract mappingMetadataContract, IntegrationSystem integrationSystem) {
        if (mappingMetadataContract.getUuid() == null) {
            throw new RuntimeException("MappingMetadata without uuid! " + mappingMetadataContract);
        }
        MappingMetaData mappingMetaData = mappingMetaDataRepository.findByUuidAndIntegrationSystem(mappingMetadataContract.getUuid(), integrationSystem);
        if (mappingMetaData == null) {
            mappingMetaData = createMappingMetadata(mappingMetadataContract);
        }
        this.updateAndSaveMappingMetadata(mappingMetaData, mappingMetadataContract, integrationSystem);
    }

    private MappingMetaData createMappingMetadata(MappingMetadataContract mappingMetadataContract) {
        MappingMetaData mappingMetaData = new MappingMetaData();
        mappingMetaData.setUuid(mappingMetadataContract.getUuid());
        return mappingMetaData;
    }

    public void updateAndSaveMappingMetadata(MappingMetaData mappingMetaData, MappingMetadataContract mappingMetadataContract, IntegrationSystem integrationSystem) {
        mappingMetaData.setIntegrationSystem(integrationSystem);
        mappingMetaData.setVoided(mappingMetadataContract.isVoided());
        mappingMetaData.setMappingGroup(mappingGroupRepository.findByUuidAndIntegrationSystem(mappingMetadataContract.getMappingGroupUuid(), integrationSystem));
        mappingMetaData.setMappingType(mappingTypeRepository.findByUuidAndIntegrationSystem(mappingMetadataContract.getMappingTypeUuid(), integrationSystem));
        mappingMetaData.setAbout(mappingMetadataContract.getAbout());
        mappingMetaData.setAvniValue(mappingMetadataContract.getAvniValue());
        mappingMetaData.setIntSystemValue(mappingMetadataContract.getIntSystemValue());
        mappingMetaData.setDataTypeHint(mappingMetadataContract.getDataTypeHint());
        mappingMetaDataRepository.save(mappingMetaData);
    }
}
