package org.avni_integration_service.integration_data.repository;

import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntegrationSystemRepository extends BaseRepository<IntegrationSystem> {
    @Deprecated //Should always use name
    IntegrationSystem findBySystemType(IntegrationSystem.IntegrationSystemType type);
    IntegrationSystem findBySystemTypeAndName(IntegrationSystem.IntegrationSystemType type, String name);
    List<IntegrationSystem> findAllBySystemType(IntegrationSystem.IntegrationSystemType integrationSystemType);
}
