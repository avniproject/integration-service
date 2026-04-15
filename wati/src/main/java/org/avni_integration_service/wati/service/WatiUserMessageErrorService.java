package org.avni_integration_service.wati.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.SendMessageResponse;
import org.avni_integration_service.integration_data.domain.error.ErrorRecord;
import org.avni_integration_service.integration_data.domain.error.ErrorType;
import org.avni_integration_service.integration_data.repository.ErrorRecordRepository;
import org.avni_integration_service.integration_data.repository.ErrorTypeRepository;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.avni_integration_service.wati.config.WatiContextProvider;
import org.avni_integration_service.wati.config.WatiEntityType;
import org.avni_integration_service.wati.config.WatiSendMsgErrorType;
import org.springframework.stereotype.Service;

@Service
public class WatiUserMessageErrorService {
    private static final Logger logger = Logger.getLogger(WatiUserMessageErrorService.class);
    private static final String EMPTY_STRING = "";

    private final ErrorRecordRepository errorRecordRepository;
    private final ErrorTypeRepository errorTypeRepository;
    private final IntegrationSystemRepository integrationSystemRepository;
    private final WatiContextProvider watiContextProvider;

    public WatiUserMessageErrorService(ErrorRecordRepository errorRecordRepository,
                                       ErrorTypeRepository errorTypeRepository,
                                       IntegrationSystemRepository integrationSystemRepository,
                                       WatiContextProvider watiContextProvider) {
        this.errorRecordRepository = errorRecordRepository;
        this.errorTypeRepository = errorTypeRepository;
        this.integrationSystemRepository = integrationSystemRepository;
        this.watiContextProvider = watiContextProvider;
    }

    public void saveUserMessageSuccess(String userId) {
        ErrorRecord errorRecord = getErrorRecord(userId);
        ErrorType errorType = getErrorType(WatiSendMsgErrorType.Success);
        saveUserMessageError(userId, errorRecord, errorType, "Processed successfully");
    }

    public void saveUserMessageError(String userId, Exception exception) {
        ErrorRecord errorRecord = getErrorRecord(userId);
        ErrorType errorType = getErrorType(WatiSendMsgErrorType.getErrorType(exception));
        saveUserMessageError(userId, errorRecord, errorType, exception.getMessage());
    }

    public void saveUserMessageStatus(String userId, SendMessageResponse sendMessageResponse) {
        ErrorRecord errorRecord = getErrorRecord(userId);
        WatiSendMsgErrorType watiSendMsgErrorType = WatiSendMsgErrorType.getErrorType(sendMessageResponse.getMessageDeliveryStatus());
        ErrorType errorType = getErrorType(watiSendMsgErrorType);
        String errorMsg = watiSendMsgErrorType.equals(WatiSendMsgErrorType.Success) ? EMPTY_STRING
                : sendMessageResponse.getErrorMessage();
        saveUserMessageError(userId, errorRecord, errorType, errorMsg);
    }

    private void saveUserMessageError(String userId, ErrorRecord errorRecord, ErrorType errorType, String errorMsg) {
        String entityType = WatiEntityType.UserMessage.name();
        if (errorRecord != null) {
            if (errorRecord.hasThisAsLastErrorTypeAndErrorMessage(errorType, errorMsg)) {
                logger.info(String.format("Same error as the last processing for userId %s, type %s", userId, entityType));
                errorRecord.updateLoggedAtForLastErrorRecordLog(null);
            } else {
                errorRecord.addErrorLog(errorType, errorMsg);
            }
        } else {
            errorRecord = new ErrorRecord();
            errorRecord.setIntegrationSystem(integrationSystemRepository.findEntity(watiContextProvider.get().getIntegrationSystem().getId()));
            errorRecord.setEntityId(userId);
            errorRecord.setIntegratingEntityType(entityType);
            errorRecord.addErrorLog(errorType, errorMsg);
            errorRecord.setProcessingDisabled(false);
        }
        errorRecordRepository.save(errorRecord);
    }

    public ErrorRecord getErrorRecord(String userId) {
        return errorRecordRepository.findByIntegratingEntityTypeAndEntityId(WatiEntityType.UserMessage.name(), userId);
    }

    private ErrorType getErrorType(WatiSendMsgErrorType watiSendMsgErrorType) {
        return errorTypeRepository.findByNameAndIntegrationSystemId(
                watiSendMsgErrorType.name(), watiContextProvider.get().getIntegrationSystem().getId());
    }
}
