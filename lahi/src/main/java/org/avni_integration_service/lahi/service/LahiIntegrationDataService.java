package org.avni_integration_service.lahi.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.integration_data.domain.IntegratingEntityStatus;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.avni_integration_service.lahi.config.LahiEntityType;
import org.avni_integration_service.lahi.domain.Student;
import org.avni_integration_service.lahi.util.DateTimeUtil;
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

    public void updateSyncStatus(Student student) {
        Date date = DateTimeUtil.toDate(student.getLastUpdatedAt(), DateTimeUtil.DATE_TIME);
        IntegratingEntityStatus integratingEntityStatus = integratingEntityStatusRepository.find(LahiEntityType.Student.name());
        integratingEntityStatus.setReadUptoDateTime(date);
        integratingEntityStatusRepository.save(integratingEntityStatus);
        logger.info(String.format("Updating integrating_entity_status with %s date", integratingEntityStatus.getReadUptoDateTime()));
    }
}
