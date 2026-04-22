package org.avni_integration_service.wati.repository;

import org.avni_integration_service.wati.domain.WatiMessageRequest;
import org.avni_integration_service.wati.domain.WatiMessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface WatiMessageRequestRepository extends JpaRepository<WatiMessageRequest, Integer> {

    List<WatiMessageRequest> findByIntegrationSystem_IdAndStatusAndNextRetryTimeLessThanEqual(
            Integer integrationSystemId, WatiMessageStatus status, LocalDateTime now);

    boolean existsByEntityIdAndFlowNameAndStatusInAndCreatedDateTimeAfterAndIntegrationSystem_Id(
            String entityId, String flowName, List<WatiMessageStatus> statuses,
            LocalDateTime cutoff, Integer integrationSystemId);
}
