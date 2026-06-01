package org.avni_integration_service.wati.repository;

import org.avni_integration_service.wati.domain.WatiMessageRequest;
import org.avni_integration_service.wati.domain.WatiMessageStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface WatiMessageRequestRepository extends JpaRepository<WatiMessageRequest, Integer> {

    List<WatiMessageRequest> findByIntegrationSystem_IdAndStatusAndNextRetryTimeLessThanEqual(
            Integer integrationSystemId, WatiMessageStatus status, LocalDateTime now, Pageable pageable);

    boolean existsByEntityIdAndFlowNameAndStatusInAndCreatedDateTimeAfterAndIntegrationSystem_Id(
            String entityId, String flowName, List<WatiMessageStatus> statuses,
            LocalDateTime cutoff, Integer integrationSystemId);

    List<WatiMessageRequest> findByIntegrationSystem_IdAndStatusAndLastAttemptTimeBefore(
            Integer integrationSystemId, WatiMessageStatus status, LocalDateTime stuckBefore);

    @Query("SELECT w.entityId FROM WatiMessageRequest w " +
           "WHERE w.flowName = :flowName " +
           "AND w.status IN :statuses " +
           "AND w.createdDateTime > :cutoff " +
           "AND w.integrationSystem.id = :integrationSystemId")
    Set<String> findEntityIdsInCooldown(
            @Param("flowName") String flowName,
            @Param("statuses") List<WatiMessageStatus> statuses,
            @Param("cutoff") LocalDateTime cutoff,
            @Param("integrationSystemId") Integer integrationSystemId);
}
