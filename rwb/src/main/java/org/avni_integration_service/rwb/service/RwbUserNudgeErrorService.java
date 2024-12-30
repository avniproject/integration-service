package org.avni_integration_service.rwb.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.integration_data.domain.error.ErrorRecord;
import org.avni_integration_service.integration_data.domain.error.ErrorType;
import org.avni_integration_service.integration_data.repository.ErrorRecordRepository;
import org.avni_integration_service.integration_data.repository.ErrorTypeRepository;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.avni_integration_service.rwb.config.RwbEntityType;
import org.avni_integration_service.rwb.config.RwbSendMsgErrorType;
import org.springframework.stereotype.Service;

@Service
public class RwbUserNudgeErrorService {
    private static final Logger logger = Logger.getLogger(RwbUserNudgeErrorService.class);

    private final ErrorRecordRepository errorRecordRepository;
    private final IntegrationSystemRepository integrationSystemRepository;
    private final ErrorTypeRepository errorTypeRepository;

    public RwbUserNudgeErrorService(ErrorRecordRepository errorRecordRepository, IntegrationSystemRepository integrationSystemRepository, ErrorTypeRepository errorTypeRepository) {
        this.errorRecordRepository = errorRecordRepository;
        this.integrationSystemRepository = integrationSystemRepository;
        this.errorTypeRepository = errorTypeRepository;
    }

    public void saveUserNudgeSuccess(String userId) {
        ErrorRecord errorRecord = getErrorRecord(userId);
        ErrorType errorType = getErrorType(RwbSendMsgErrorType.Success);
        saveUserNudgeError(userId, errorRecord, errorType, "Processed successfully");
    }

    public void saveUserNudgeError(String userId, Exception exception) {
        ErrorRecord errorRecord = getErrorRecord(userId);
        ErrorType errorType = getErrorType(RwbSendMsgErrorType.getErrorType(exception));
        String errorMsg = exception.getMessage();
        saveUserNudgeError(userId, errorRecord, errorType, errorMsg);
    }

    private void saveUserNudgeError(String userId, ErrorRecord errorRecord, ErrorType errorType, String errorMsg) {
        String rwbEntityType = RwbEntityType.UserNudge.name();
        if (errorRecord != null) {
            if (errorRecord.hasThisAsLastErrorTypeAndErrorMessage(errorType, errorMsg)) {
                logger.info(String.format("Same error as the last processing for entity userIdValue %s, and type %s", userId, rwbEntityType));
                errorRecord.updateLoggedAtForLastErrorRecordLog(null);
            } else {
                errorRecord.addErrorLog(errorType, errorMsg);
            }
        } else {
            errorRecord = new ErrorRecord();
            errorRecord.setIntegrationSystem(integrationSystemRepository.find());
            errorRecord.setEntityId(userId);
            errorRecord.setIntegratingEntityType(rwbEntityType);
            errorRecord.addErrorLog(errorType, errorMsg);
            errorRecord.setProcessingDisabled(false);
        }
        errorRecordRepository.save(errorRecord);
    }

    public ErrorRecord getErrorRecord(String userId) {
        return errorRecordRepository.findByIntegratingEntityTypeAndEntityId(RwbEntityType.UserNudge.name(), userId);
    }

    private ErrorType getErrorType(RwbSendMsgErrorType rwbSendMsgErrorType) {
        String name = rwbSendMsgErrorType.name();
        return errorTypeRepository.findByNameAndIntegrationSystem(name, integrationSystemRepository.find());
    }
}

