package org.avni_integration_service.lahi.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.integration_data.domain.IntegratingEntityStatus;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.avni_integration_service.lahi.domain.LahiStudent;
import org.avni_integration_service.lahi.util.DateTimeUtil;
import org.springframework.stereotype.Service;

import java.util.Date;

import static org.avni_integration_service.lahi.service.LahiStudentService.ENTITYTYPE;

@Service
public class LahiIntegrationDataService {
    private static final Logger logger = Logger.getLogger(LahiIntegrationDataService.class);
    private final IntegratingEntityStatusRepository integratingEntityStatusRepository;

    public LahiIntegrationDataService(IntegratingEntityStatusRepository integratingEntityStatusRepository) {
        this.integratingEntityStatusRepository = integratingEntityStatusRepository;
    }

    public void studentProcessed(LahiStudent student) {
        Date date = DateTimeUtil.toDate(student.getLastUpdatedAt(), DateTimeUtil.DATE_TIME);
        IntegratingEntityStatus integratingEntityStatus = integratingEntityStatusRepository.findByEntityType(ENTITYTYPE);
        integratingEntityStatus.setReadUptoDateTime(date);
        integratingEntityStatusRepository.save(integratingEntityStatus);
        logger.info(String.format("Updating integrating_entity_status with %s date", integratingEntityStatus.getReadUptoDateTime()));
    }
}
