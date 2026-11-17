package org.avni_integration_service.tanuh.repository;

import org.avni_integration_service.tanuh.domain.TanuhOcshrCase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TanuhOcshrCaseRepository extends JpaRepository<TanuhOcshrCase, Integer> {
    TanuhOcshrCase findByIntegrationSystem_IdAndScreeningUuid(Integer integrationSystemId, String screeningUuid);
}
