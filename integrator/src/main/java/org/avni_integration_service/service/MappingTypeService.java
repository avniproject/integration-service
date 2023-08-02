package org.avni_integration_service.service;

import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.MappingType;
import org.avni_integration_service.integration_data.repository.MappingTypeRepository;
import org.avni_integration_service.web.contract.NamedEntityContract;
import org.springframework.stereotype.Service;

@Service
public class MappingTypeService {
    private final MappingTypeRepository mappingTypeRepository;

    public MappingTypeService(MappingTypeRepository mappingTypeRepository) {
        this.mappingTypeRepository = mappingTypeRepository;
    }

    public void createOrUpdateMappingType(NamedEntityContract mappingTypeContract, IntegrationSystem integrationSystem) {
        if (mappingTypeContract.getUuid() == null) {
            throw new RuntimeException("MappingType without uuid! " + mappingTypeContract);
        }
        MappingType mappingType = mappingTypeRepository.findByUuidAndIntegrationSystem(mappingTypeContract.getUuid(), integrationSystem);
        if (mappingType == null) {
            mappingType = createMappingType(mappingTypeContract);
        }
        this.updateAndSaveMappingType(mappingType, mappingTypeContract, integrationSystem);
    }

    private MappingType createMappingType(NamedEntityContract mappingTypeContract) {
        MappingType mappingType = new MappingType();
        mappingType.setUuid(mappingTypeContract.getUuid());
        return mappingType;
    }

    public void updateAndSaveMappingType(MappingType mappingType, NamedEntityContract mappingTypeContract, IntegrationSystem integrationSystem) {
        mappingType.setIntegrationSystem(integrationSystem);
        mappingType.setName(mappingTypeContract.getName());
        mappingType.setVoided(mappingTypeContract.isVoided());
        mappingTypeRepository.save(mappingType);
    }
}

