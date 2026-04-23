package org.avni_integration_service.wati.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.integration_data.domain.error.ErrorRecord;
import org.avni_integration_service.integration_data.domain.error.ErrorType;
import org.avni_integration_service.integration_data.repository.ErrorRecordRepository;
import org.avni_integration_service.integration_data.repository.ErrorTypeRepository;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.avni_integration_service.wati.config.WatiContextProvider;
import org.avni_integration_service.wati.domain.WatiMessageRequest;
import org.springframework.stereotype.Service;

@Service
public class WatiErrorService {

    public static final String PERMANENT_FAILURE_ERROR_TYPE = "WatiMessagePermanentFailure";

    private static final Logger logger = Logger.getLogger(WatiErrorService.class);

    private final ErrorRecordRepository errorRecordRepository;
    private final ErrorTypeRepository errorTypeRepository;
    private final IntegrationSystemRepository integrationSystemRepository;
    private final WatiContextProvider watiContextProvider;

    public WatiErrorService(ErrorRecordRepository errorRecordRepository,
                            ErrorTypeRepository errorTypeRepository,
                            IntegrationSystemRepository integrationSystemRepository,
                            WatiContextProvider watiContextProvider) {
        this.errorRecordRepository = errorRecordRepository;
        this.errorTypeRepository = errorTypeRepository;
        this.integrationSystemRepository = integrationSystemRepository;
        this.watiContextProvider = watiContextProvider;
    }

    public void reportPermanentFailure(WatiMessageRequest request) {
        int integrationSystemId = watiContextProvider.get().getIntegrationSystem().getId();
        ErrorType errorType = errorTypeRepository.findByNameAndIntegrationSystemId(PERMANENT_FAILURE_ERROR_TYPE, integrationSystemId);
        if (errorType == null) {
            logger.warn(String.format("ErrorType '%s' not found for integrationSystem %d — skipping error record",
                    PERMANENT_FAILURE_ERROR_TYPE, integrationSystemId));
            return;
        }

        String entityId = request.getEntityId();
        String entityType = request.getEntityType();
        ErrorRecord errorRecord = errorRecordRepository.findByIntegratingEntityTypeAndEntityId(entityType, entityId);

        if (errorRecord != null && errorRecord.hasThisAsLastErrorTypeAndErrorMessage(errorType, request.getErrorMessage())) {
            errorRecord.updateLoggedAtForLastErrorRecordLog(null);
            errorRecordRepository.save(errorRecord);
        } else if (errorRecord != null) {
            errorRecord.addErrorLog(errorType, request.getErrorMessage());
            errorRecordRepository.save(errorRecord);
        } else {
            errorRecord = new ErrorRecord();
            errorRecord.setIntegratingEntityType(entityType);
            errorRecord.setEntityId(entityId);
            errorRecord.addErrorLog(errorType, request.getErrorMessage());
            errorRecord.setProcessingDisabled(false);
            errorRecord.setIntegrationSystem(
                    integrationSystemRepository.findEntity(integrationSystemId));
            errorRecordRepository.save(errorRecord);
        }
        logger.info(String.format("Reported permanent failure for entity %s (type: %s, flow: %s)",
                entityId, entityType, request.getFlowName()));
    }
}
