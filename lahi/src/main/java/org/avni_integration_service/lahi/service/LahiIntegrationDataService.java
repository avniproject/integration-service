package org.avni_integration_service.lahi.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.integration_data.domain.IntegratingEntityStatus;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class LahiIntegrationDataService {
    private static final Logger logger = Logger.getLogger(LahiIntegrationDataService.class);
    private final IntegratingEntityStatusRepository integratingEntityStatusRepository;

    public LahiIntegrationDataService(IntegratingEntityStatusRepository integratingEntityStatusRepository) {
        this.integratingEntityStatusRepository = integratingEntityStatusRepository;
    }

    public void updateSyncStatus(@NonNull String entityType, Date readUptoDateTime, boolean shouldUpdate) {
        if(!shouldUpdate) {
            return;
        }

        IntegratingEntityStatus integratingEntityStatus = integratingEntityStatusRepository.find(entityType);
        integratingEntityStatus.setReadUptoDateTime(readUptoDateTime);
        integratingEntityStatusRepository.save(integratingEntityStatus);
        logger.info(String.format("Updating integrating_entity_status with %s date", integratingEntityStatus.getReadUptoDateTime()));
    }
}
