package org.avni_integration_service.integration_data.repository.framework;

import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RepositoryProvider {
    private static IntegrationSystemRepository integrationSystemRepository;

    @Autowired
    public RepositoryProvider(IntegrationSystemRepository integrationSystemRepository) {
        this.integrationSystemRepository = integrationSystemRepository;
    }

    public static IntegrationSystemRepository getIntegrationSystemRepository() {
        return integrationSystemRepository;
    }
}
