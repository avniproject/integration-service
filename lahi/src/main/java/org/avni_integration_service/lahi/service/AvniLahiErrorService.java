package org.avni_integration_service.lahi.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.lahi.config.LahiEntityType;
import org.avni_integration_service.lahi.config.LahiErrorType;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.error.ErrorRecord;
import org.avni_integration_service.integration_data.domain.error.ErrorType;
import org.avni_integration_service.integration_data.repository.ErrorRecordRepository;
import org.avni_integration_service.integration_data.repository.ErrorTypeRepository;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class AvniLahiErrorService {
    private static final Logger logger = Logger.getLogger(AvniLahiErrorService.class);

    private final ErrorRecordRepository errorRecordRepository;
    private final IntegrationSystemRepository integrationSystemRepository;
    private final ErrorTypeRepository errorTypeRepository;

    @Autowired
    public AvniLahiErrorService(ErrorRecordRepository errorRecordRepository, IntegrationSystemRepository integrationSystemRepository, ErrorTypeRepository errorTypeRepository) {
        this.errorRecordRepository = errorRecordRepository;
        this.integrationSystemRepository = integrationSystemRepository;
        this.errorTypeRepository = errorTypeRepository;
    }

    private ErrorType getErrorType(LahiErrorType lahiErrorType) {
        return errorTypeRepository.findByNameAndIntegrationSystem(lahiErrorType.name(), integrationSystemRepository.find());
    }

    public List<ErrorType> getUnprocessableErrorTypes() {
        // todo
        return  Collections.emptyList();
    }

    private ErrorRecord saveExotelError(String uuid, LahiErrorType lahiErrorType, LahiEntityType lahiEntityType) {
        ErrorRecord errorRecord = errorRecordRepository.findByIntegratingEntityTypeAndEntityId(lahiEntityType.name(), uuid);
        if (errorRecord != null && errorRecord.hasThisAsLastErrorType(getErrorType(lahiErrorType))) {
            logger.info(String.format("Same error as the last processing for entity uuid %s, and type %s", uuid, lahiEntityType));
            if (!errorRecord.isProcessingDisabled()) {
                errorRecord.setProcessingDisabled(true);
                errorRecordRepository.save(errorRecord);
            }
        } else if (errorRecord != null && !errorRecord.hasThisAsLastErrorType(getErrorType(lahiErrorType))) {
            logger.info(String.format("New error for entity uuid %s, and type %s", uuid, lahiEntityType));
            errorRecord.addErrorType(getErrorType(lahiErrorType));
            errorRecordRepository.save(errorRecord);
        } else {
            errorRecord = new ErrorRecord();
            errorRecord.setIntegratingEntityType(lahiEntityType.name());
            errorRecord.setEntityId(uuid);
            errorRecord.addErrorType(getErrorType(lahiErrorType));
            errorRecord.setProcessingDisabled(false);
            errorRecord.setIntegrationSystem(integrationSystemRepository.find());
            errorRecordRepository.save(errorRecord);
        }
        return errorRecord;
    }

    public ErrorRecord errorOccurred(String entityUuid, LahiErrorType lahiErrorType, LahiEntityType lahiEntityType) {
        return saveExotelError(entityUuid, lahiErrorType, lahiEntityType);
    }

    private void successfullyProcessedLahiEntity(LahiEntityType lahiEntityType, String uuid) {
        ErrorRecord errorRecord = errorRecordRepository.findByIntegratingEntityTypeAndEntityId(lahiEntityType.name(), uuid);
        if (errorRecord != null)
            errorRecordRepository.delete(errorRecord);
    }

    public void successfullyProcessed(String entityUUID, LahiEntityType lahiEntityType) {
        successfullyProcessedLahiEntity(lahiEntityType, entityUUID);
    }
}
