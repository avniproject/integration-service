package org.avni_integration_service.integration_data.repository;

import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.MappingType;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MappingTypeRepository extends BaseRepository<MappingType> {
    MappingType findByName(String name);
    MappingType findByNameAndIsVoidedFalse(String name);
    MappingType findByUuidAndIntegrationSystem(String uuid, IntegrationSystem integrationSystem);
    List<MappingType> findAllByIntegrationSystem(IntegrationSystem integrationSystem);
}
