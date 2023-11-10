package org.avni_integration_service.integration_data.repository;

import org.avni_integration_service.integration_data.context.IntegrationContext;
import org.avni_integration_service.integration_data.domain.IntegratingEntityStatus;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.repository.framework.RepositoryProvider;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IntegratingEntityStatusRepository extends CrudRepository<IntegratingEntityStatus, Integer> {
    @Deprecated // use the method find
    IntegratingEntityStatus findByEntityType(String entityType);

    IntegratingEntityStatus findByEntityTypeAndIntegrationSystem(String entityType, IntegrationSystem integrationSystem);

    default IntegratingEntityStatus find(String entityType) {
        return findByEntityTypeAndIntegrationSystem(entityType, getIntegrationSystem());
    }

    private IntegrationSystem getIntegrationSystem() {
        return RepositoryProvider.getIntegrationSystemRepository().findById(IntegrationContext.getIntegrationSystemId()).get();
    }
}
