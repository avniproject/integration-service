package org.avni_integration_service.rwb.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.SendMessageResponse;
import org.avni_integration_service.integration_data.domain.error.ErrorRecord;
import org.avni_integration_service.integration_data.domain.error.ErrorType;
import org.avni_integration_service.integration_data.repository.ErrorRecordRepository;
import org.avni_integration_service.integration_data.repository.ErrorTypeRepository;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.avni_integration_service.rwb.config.RwbContextProvider;
import org.avni_integration_service.rwb.config.RwbEntityType;
import org.avni_integration_service.rwb.config.RwbSendMsgErrorType;
import org.springframework.stereotype.Service;

@Service
public class RwbUserNudgeErrorService {
    private static final Logger logger = Logger.getLogger(RwbUserNudgeErrorService.class);
    private static final String EMPTY_STRING = "";

    private final ErrorRecordRepository errorRecordRepository;
    private final ErrorTypeRepository errorTypeRepository;
    private final IntegrationSystemRepository integrationSystemRepository;
    private final RwbContextProvider rwbContextProvider;

    public RwbUserNudgeErrorService(ErrorRecordRepository errorRecordRepository, ErrorTypeRepository errorTypeRepository, IntegrationSystemRepository integrationSystemRepository, RwbContextProvider rwbContextProvider) {
        this.errorRecordRepository = errorRecordRepository;
        this.errorTypeRepository = errorTypeRepository;
        this.integrationSystemRepository = integrationSystemRepository;
        this.rwbContextProvider = rwbContextProvider;
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

    public void saveUserNudgeStatus(String userId, SendMessageResponse sendMessageResponse) {
        ErrorRecord errorRecord = getErrorRecord(userId);
        RwbSendMsgErrorType rwbSendMsgErrorType = RwbSendMsgErrorType.getErrorType(sendMessageResponse.getMessageDeliveryStatus());
        ErrorType errorType = getErrorType(rwbSendMsgErrorType);
        String errorMsg = rwbSendMsgErrorType.equals(RwbSendMsgErrorType.Success) ? EMPTY_STRING
                : sendMessageResponse.getErrorMessage();
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
            errorRecord.setIntegrationSystem(integrationSystemRepository.findEntity(rwbContextProvider.get().getIntegrationSystem().getId()));
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
        return errorTypeRepository.findByNameAndIntegrationSystemId(name, rwbContextProvider.get().getIntegrationSystem().getId());
    }
}

