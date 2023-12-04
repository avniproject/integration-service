package org.avni_integration_service.integration_data.repository;

import org.avni_integration_service.integration_data.context.IntegrationContext;
import org.avni_integration_service.integration_data.domain.AvniEntityType;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.error.ErrorRecord;
import org.avni_integration_service.integration_data.domain.error.ErrorType;
import org.avni_integration_service.integration_data.repository.framework.RepositoryProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Stream;

@Repository
public interface ErrorRecordRepository extends PagingAndSortingRepository<ErrorRecord, Integer> {
    ErrorRecord findByAvniEntityTypeAndEntityId(AvniEntityType avniEntityType, String entityId);
    ErrorRecord findByIntegratingEntityTypeAndEntityId(String integratingEntityType, String entityId);

    Page<ErrorRecord> findAllByAvniEntityTypeNotNullAndProcessingDisabledFalseAndErrorRecordLogsErrorTypeNotInOrderById(List<ErrorType> errorTypes, Pageable pageable);
    Page<ErrorRecord> findAllByIntegratingEntityTypeNotNullAndProcessingDisabledFalseAndErrorRecordLogsErrorTypeNotInOrderById(List<ErrorType> errorTypes, Pageable pageable);
    Page<ErrorRecord> findAllByIntegratingEntityTypeNotNullAndErrorRecordLogsErrorTypeNotInOrderById(List<ErrorType> errorTypes, Pageable pageable);
    Page<ErrorRecord> findAllByAvniEntityTypeNotNullAndProcessingDisabledFalseAndErrorRecordLogsErrorTypeNotInAndIntegrationSystemIdOrderById(List<ErrorType> errorTypes, int integrationSystemId, Pageable pageable);
    Page<ErrorRecord> findAllByIntegratingEntityTypeNotNullAndProcessingDisabledFalseAndErrorRecordLogsErrorTypeNotInAndIntegrationSystemIdOrderById(List<ErrorType> errorTypes, int integrationSystemId, Pageable pageable);
    Page<ErrorRecord> findAllByIntegratingEntityTypeNotNullAndErrorRecordLogsErrorTypeNotInAndIntegrationSystemOrderById(List<ErrorType> errorTypes, IntegrationSystem integrationSystem, Pageable pageable);
    Page<ErrorRecord> findAllByIntegratingEntityTypeNotNullAndErrorRecordLogsErrorTypeNotInAndIntegrationSystemIdOrderById(List<ErrorType> errorTypes, int integrationSystemId, Pageable pageable);

    List<ErrorRecord> findAllByAvniEntityTypeNotNull();

    default ErrorRecord saveErrorRecord(ErrorRecord errorRecord) {
        errorRecord.setIntegrationSystem(getIntegrationSystem());
        return this.save(errorRecord);
    }

    private IntegrationSystem getIntegrationSystem() {
        return RepositoryProvider.getIntegrationSystemRepository().findById(IntegrationContext.getIntegrationSystemId()).get();
    }

    default Stream<ErrorRecord> getProcessableErrorRecords() {
        return findAllByProcessingDisabledFalseAndIntegrationSystem(getIntegrationSystem());
    }
    Stream<ErrorRecord> findAllByProcessingDisabledFalseAndIntegrationSystem(IntegrationSystem integrationSystem);
}
