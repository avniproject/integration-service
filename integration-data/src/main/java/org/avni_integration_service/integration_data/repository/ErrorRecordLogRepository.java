package org.avni_integration_service.integration_data.repository;

import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.error.ErrorRecordLog;
import org.avni_integration_service.integration_data.domain.error.ErrorType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public interface ErrorRecordLogRepository extends PagingAndSortingRepository<ErrorRecordLog, Integer> {
    Page<ErrorRecordLog> findAllByErrorRecordEntityIdContainsAndErrorRecordIntegrationSystem(String entityId, IntegrationSystem integrationSystem,
                                                                                             Pageable pageable);
    Page<ErrorRecordLog> findAllByErrorTypeAndErrorRecordIntegrationSystem(ErrorType errorType, IntegrationSystem integrationSystem,
                                                                           Pageable pageable);
    Page<ErrorRecordLog> findAllByErrorTypeAndErrorRecordEntityIdContainsAndErrorRecordIntegrationSystem(ErrorType errorType, String entityId,
                                                                                                         IntegrationSystem integrationSystem,
                                                                                                         Pageable pageable);
    Page<ErrorRecordLog> findAllByLoggedAtAfterAndLoggedAtBeforeAndErrorRecordIntegrationSystem(Date start, Date endDate,
                                                                                                IntegrationSystem integrationSystem,
                                                                                                Pageable pageable);
    Page<ErrorRecordLog> findAllByLoggedAtBeforeAndErrorRecordIntegrationSystem(Date endDate, IntegrationSystem integrationSystem,
                                                                                Pageable pageable);
    Page<ErrorRecordLog> findAllByLoggedAtAfterAndErrorRecordIntegrationSystem(Date start, IntegrationSystem integrationSystem, Pageable pageable);
    Page<ErrorRecordLog> findAllByErrorRecordIntegrationSystem(IntegrationSystem currentIntegrationSystem, Pageable pageable);

    @Query(value = "select count(erl.*) from error_record_log erl join error_type et on erl.error_type_id = et.id join error_record er on erl.error_record_id = er.id where erl.logged_at >= :startDate and erl.logged_at <= :endDate and et.follow_up_step = :errorTypeFollowUpStep and er.integration_system_id = :integrationSystemId", nativeQuery = true)
    long countByLoggedAtIsBetweenAndErrorTypeFollowUpStepAndErrorRecordIntegrationSystemId(
            Date startDate, Date endDate, String errorTypeFollowUpStep, int integrationSystemId);

    ErrorRecordLog findTopByOrderByLoggedAtDesc();
}
