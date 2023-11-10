package org.avni_integration_service.integration_data.repository;

import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.error.ErrorType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ErrorTypeRepository extends BaseRepository<ErrorType> {
    ErrorType findByNameAndIntegrationSystem(String name, IntegrationSystem integrationSystem);
    ErrorType findByUuidAndIntegrationSystem(String uuid, IntegrationSystem integrationSystem);
    ErrorType findByNameAndIntegrationSystemId(String name, int integrationSystemId);
    @Query(value = "select * from error_type et where et.follow_up_step = :errorTypeFollowUpStep and et.integration_system_id = :integrationSystemId", nativeQuery = true)
    List<ErrorType> findByIntegrationSystemIdAndFollowUpStep(int integrationSystemId, String errorTypeFollowUpStep);
    List<ErrorType> findAllByIntegrationSystemId(int id);
    List<ErrorType> findAllByIntegrationSystem(IntegrationSystem integrationSystem);
}
