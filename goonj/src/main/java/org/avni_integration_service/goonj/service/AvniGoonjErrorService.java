package org.avni_integration_service.goonj.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.goonj.GoonjEntityType;
import org.avni_integration_service.goonj.config.GoonjContextProvider;
import org.avni_integration_service.integration_data.domain.AvniEntityType;
import org.avni_integration_service.integration_data.domain.error.ErrorRecord;
import org.avni_integration_service.integration_data.domain.error.ErrorType;
import org.avni_integration_service.integration_data.domain.error.ErrorTypeFollowUpStep;
import org.avni_integration_service.integration_data.repository.ErrorRecordRepository;
import org.avni_integration_service.integration_data.repository.ErrorTypeRepository;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AvniGoonjErrorService {
    private static final Logger logger = Logger.getLogger(AvniGoonjErrorService.class);

    private final ErrorRecordRepository errorRecordRepository;
    private final IntegrationSystemRepository integrationSystemRepository;
    private final ErrorTypeRepository errorTypeRepository;
    private final GoonjContextProvider goonjContextProvider;

    @Autowired
    public AvniGoonjErrorService(ErrorRecordRepository errorRecordRepository, IntegrationSystemRepository integrationSystemRepository, ErrorTypeRepository errorTypeRepository, GoonjContextProvider goonjContextProvider) {
        this.errorRecordRepository = errorRecordRepository;
        this.integrationSystemRepository = integrationSystemRepository;
        this.errorTypeRepository = errorTypeRepository;
        this.goonjContextProvider = goonjContextProvider;
    }

    public List<ErrorType> getUnprocessableErrorTypes() {
        return getErrorTypeBy(ErrorTypeFollowUpStep.Terminal);
    }


    private void saveAvniError(String uuid, String goonjErrorTypeName, AvniEntityType avniEntityType, String errorMsg, Map<String, Object> errorBody) {
        ErrorType errorType = getErrorType(goonjErrorTypeName);
        ErrorRecord errorRecord = errorRecordRepository.findByAvniEntityTypeAndEntityId(avniEntityType, uuid);
        if (errorRecord != null && errorRecord.hasThisAsLastErrorTypeAndErrorMessage(errorType, errorMsg)) {
            logger.info(String.format("Same error as the last processing for entity uuid %s, and type %s", uuid, avniEntityType));
            if (!errorRecord.isProcessingDisabled()) {
                errorRecord.setProcessingDisabled(true);
                errorRecord.updateLoggedAtForLastErrorRecordLog(errorBody);
                errorRecordRepository.save(errorRecord);
            }
        } else if (errorRecord != null && !errorRecord.hasThisAsLastErrorTypeAndErrorMessage(errorType, errorMsg)) {
            errorRecord.addErrorLog(errorType, errorMsg,errorBody);
            errorRecordRepository.save(errorRecord);
        } else {
            errorRecord = new ErrorRecord();
            errorRecord.setAvniEntityType(avniEntityType);
            errorRecord.setEntityId(uuid);
            errorRecord.addErrorLog(errorType, errorMsg,errorBody);
            errorRecord.setProcessingDisabled(false);
            errorRecord.setIntegrationSystem(integrationSystemRepository.findEntity(goonjContextProvider.get().getIntegrationSystem().getId()));
            errorRecordRepository.save(errorRecord);
        }
    }

    private ErrorType getErrorType(String goonjErrorTypeName) {
        return errorTypeRepository.findByNameAndIntegrationSystemId(goonjErrorTypeName, goonjContextProvider.get().getIntegrationSystem().getId());
    }

    private List<ErrorType> getErrorTypeBy(ErrorTypeFollowUpStep followUpStep) {
        return errorTypeRepository.findByIntegrationSystemIdAndFollowUpStep(
                goonjContextProvider.get().getIntegrationSystem().getId(), String.valueOf(followUpStep.ordinal()));
    }

    private ErrorRecord saveGoonjError(String uuid, String goonjErrorTypeName, GoonjEntityType goonjEntityType, String errorMsg, Map<String, Object> body) {
        ErrorRecord errorRecord = errorRecordRepository.findByIntegratingEntityTypeAndEntityId(goonjEntityType.name(), uuid);
        ErrorType errorType = getErrorType(goonjErrorTypeName);
        if (errorRecord != null && errorRecord.hasThisAsLastErrorTypeAndErrorMessage(errorType, errorMsg)) {
            logger.info(String.format("Same error as the last processing for entity uuid %s, and type %s", uuid, goonjEntityType));
            errorRecord.updateLoggedAtForLastErrorRecordLog(body);
            errorRecordRepository.save(errorRecord);
        } else if (errorRecord != null && !errorRecord.hasThisAsLastErrorTypeAndErrorMessage(errorType, errorMsg)) {
            logger.info(String.format("New error for entity uuid %s, and type %s", uuid, goonjEntityType));
            errorRecord.addErrorLog(errorType, errorMsg,body);
            errorRecordRepository.save(errorRecord);
        } else {
            errorRecord = new ErrorRecord();
            errorRecord.setIntegratingEntityType(goonjEntityType.name());
            errorRecord.setEntityId(uuid);
            errorRecord.addErrorLog(errorType, errorMsg,body);
            errorRecord.setProcessingDisabled(false);
            errorRecord.setIntegrationSystem(integrationSystemRepository.findEntity(goonjContextProvider.get().getIntegrationSystem().getId()));
            errorRecordRepository.save(errorRecord);
        }
        return errorRecord;
    }


    public ErrorRecord errorOccurred(String entityUuid, String goonjErrorTypeName, GoonjEntityType goonjEntityType, String errorMsg, Map<String, Object> errorBody) {
        return saveGoonjError(entityUuid, goonjErrorTypeName, goonjEntityType, errorMsg, errorBody);
    }

    public void errorOccurred(String entityUuid, String goonjErrorTypeName, AvniEntityType avniEntityType, String errorMsg, Map<String, Object> errorBody) {
        saveAvniError(entityUuid, goonjErrorTypeName, avniEntityType, errorMsg,errorBody);
    }

    private void successfullyProcessedGoonjEntity(GoonjEntityType goonjEntityType, String uuid) {
        ErrorRecord errorRecord = errorRecordRepository.findByIntegratingEntityTypeAndEntityId(goonjEntityType.name(), uuid);
        if (errorRecord != null)
            errorRecordRepository.delete(errorRecord);
    }

    public void successfullyProcessed(String entityUUID, GoonjEntityType goonjEntityType) {
        successfullyProcessedGoonjEntity(goonjEntityType, entityUUID);
    }
}
