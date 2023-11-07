package org.avni_integration_service.integration_data.repository;

import org.avni_integration_service.integration_data.context.ContextIntegrationSystem;
import org.avni_integration_service.integration_data.context.IntegrationContext;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntegrationSystemRepository extends BaseRepository<IntegrationSystem> {
    @Deprecated //Should always use name
    IntegrationSystem findBySystemType(IntegrationSystem.IntegrationSystemType type);
    IntegrationSystem findBySystemTypeAndName(IntegrationSystem.IntegrationSystemType type, String name);

    default IntegrationSystem find() {
        return this.find(IntegrationContext.get());
    }

    default IntegrationSystem find(ContextIntegrationSystem contextIntegrationSystem) {
        return this.findBySystemTypeAndName(contextIntegrationSystem.getSystemType(), contextIntegrationSystem.getName());
    }
    List<IntegrationSystem> findAllBySystemType(IntegrationSystem.IntegrationSystemType integrationSystemType);
}
