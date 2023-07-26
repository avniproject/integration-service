package org.avni_integration_service.service;

import org.avni_integration_service.integration_data.domain.IntegrationSystem;

import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfig;
import org.avni_integration_service.integration_data.repository.config.IntegrationSystemConfigRepository;
import org.avni_integration_service.web.contract.IntegrationSystemConfigContract;
import org.springframework.stereotype.Service;

@Service
public class IntegrationSystemConfigService {
    private final IntegrationSystemConfigRepository integrationSystemConfigRepository;

    public IntegrationSystemConfigService(IntegrationSystemConfigRepository integrationSystemConfigRepository) {
        this.integrationSystemConfigRepository = integrationSystemConfigRepository;
    }

    public void createOrUpdateIntegrationSystemConfig(IntegrationSystemConfigContract integrationSystemConfigContract, IntegrationSystem integrationSystem) {
        if (integrationSystemConfigContract.getUuid() == null) {
            throw new RuntimeException("IntegrationSystemConfig without uuid! " + integrationSystemConfigContract);
        }
        IntegrationSystemConfig integrationSystemConfig = integrationSystemConfigRepository.findByUuid(integrationSystemConfigContract.getUuid());
        if (integrationSystemConfig == null) {
            integrationSystemConfig = createIntegrationSystemConfig(integrationSystemConfigContract);
        } else {
            if (integrationSystemConfig.isSecret()) return; //Don't overwrite existing secret configs from bundle
        }
        this.updateAndSaveIntegrationSystemConfig(integrationSystemConfig, integrationSystemConfigContract, integrationSystem);
    }

    public IntegrationSystemConfig createIntegrationSystemConfig(IntegrationSystemConfigContract integrationSystemConfigContract) {
        IntegrationSystemConfig integrationSystemConfig = new IntegrationSystemConfig();
        integrationSystemConfig.setUuid(integrationSystemConfigContract.getUuid());
        return integrationSystemConfig;
    }

    public void updateAndSaveIntegrationSystemConfig(IntegrationSystemConfig integrationSystemConfig, IntegrationSystemConfigContract integrationSystemConfigContract, IntegrationSystem integrationSystem) {
        integrationSystemConfig.setIntegrationSystem(integrationSystem);
        integrationSystemConfig.setVoided(integrationSystemConfigContract.isVoided());
        integrationSystemConfig.setKey(integrationSystemConfigContract.getKey());
        integrationSystemConfig.setValue(integrationSystemConfigContract.getValue());
        integrationSystemConfig.setSecret(integrationSystemConfigContract.isSecret());
        integrationSystemConfigRepository.save(integrationSystemConfig);
    }
}
