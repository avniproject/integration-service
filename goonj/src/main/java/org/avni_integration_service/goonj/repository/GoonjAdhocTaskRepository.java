package org.avni_integration_service.goonj.repository;

import org.avni_integration_service.goonj.domain.GoonjAdhocTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.persistence.Id;
import java.util.List;
import java.util.Optional;

@Repository
public interface GoonjAdhocTaskRepository extends JpaRepository<GoonjAdhocTask, Id> {
    Optional<GoonjAdhocTask> findByUuid(String uuid);
    @Query("SELECT t FROM GoonjAdhocTask t WHERE t.isVoided = false")
    List<GoonjAdhocTask> findAllByVoidedIsFalse();
}
