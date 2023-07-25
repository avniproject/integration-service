package org.avni_integration_service.service;

import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.MappingGroup;
import org.avni_integration_service.integration_data.repository.MappingGroupRepository;
import org.avni_integration_service.web.contract.NamedEntityContract;
import org.springframework.stereotype.Service;

@Service
public class MappingGroupService {
    private final MappingGroupRepository mappingGroupRepository;

    public MappingGroupService(MappingGroupRepository mappingGroupRepository) {
        this.mappingGroupRepository = mappingGroupRepository;
    }

    public void createOrUpdateMappingGroup(NamedEntityContract mappingGroupContract, IntegrationSystem integrationSystem) {
        if (mappingGroupContract.getUuid() == null) {
            throw new RuntimeException("MappingGroup without uuid! " + mappingGroupContract);
        }
        MappingGroup mappingGroup = mappingGroupRepository.findByUuid(mappingGroupContract.getUuid());
        if (mappingGroup == null) {
            mappingGroup = createMappingGroup(mappingGroupContract);
        }
        this.updateAndSaveMappingGroup(mappingGroup, mappingGroupContract, integrationSystem);
    }

    private MappingGroup createMappingGroup(NamedEntityContract mappingGroupContract) {
        MappingGroup mappingGroup = new MappingGroup();
        mappingGroup.setUuid(mappingGroupContract.getUuid());
        return mappingGroup;
    }

    public void updateAndSaveMappingGroup(MappingGroup mappingGroup, NamedEntityContract mappingGroupContract, IntegrationSystem integrationSystem) {
        mappingGroup.setIntegrationSystem(integrationSystem);
        mappingGroup.setName(mappingGroupContract.getName());
        mappingGroup.setVoided(mappingGroupContract.isVoided());
        mappingGroupRepository.save(mappingGroup);
    }
}

