package org.avni_integration_service.tanuh.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.integration_data.domain.AvniEntityType;
import org.avni_integration_service.integration_data.domain.error.ErrorRecord;
import org.avni_integration_service.integration_data.domain.error.ErrorRecordLog;
import org.avni_integration_service.integration_data.domain.error.ErrorType;
import org.avni_integration_service.integration_data.repository.ErrorRecordRepository;
import org.avni_integration_service.integration_data.repository.ErrorTypeRepository;
import org.avni_integration_service.tanuh.config.TanuhContextProvider;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
public class TanuhErrorService {
    public static final String MODEL_CALL_FAILED = "HighRiskModelCallFailed";
    public static final String SCREENING_PROCESSING_FAILED = "ScreeningProcessingFailed";
    private static final Logger logger = Logger.getLogger(TanuhErrorService.class);

    private final ErrorRecordRepository errorRecordRepository;
    private final ErrorTypeRepository errorTypeRepository;
    private final TanuhContextProvider tanuhContextProvider;

    public TanuhErrorService(ErrorRecordRepository errorRecordRepository, ErrorTypeRepository errorTypeRepository, TanuhContextProvider tanuhContextProvider) {
        this.errorRecordRepository = errorRecordRepository;
        this.errorTypeRepository = errorTypeRepository;
        this.tanuhContextProvider = tanuhContextProvider;
    }

    // One record per screening, on the Avni-entity column the main job's skip reads, so the retry job (#132) finds
    // exactly one. ErrorRecordLog is equal by record and error type, so a record keeps one log per type: a later
    // failure of the same type refreshes that log's message and time rather than adding one the set would drop.
    public void errorOccurred(String uuid, String errorTypeName, String message) {
        int integrationSystemId = tanuhContextProvider.get().getIntegrationSystem().getId();
        ErrorType errorType = errorTypeRepository.findByNameAndIntegrationSystemId(errorTypeName, integrationSystemId);
        if (errorType == null)
            throw new IllegalStateException(String.format("No error type %s for integration system %d. Run the organisation's setup script.", errorTypeName, integrationSystemId));
        ErrorRecord errorRecord = errorRecordRepository.findByAvniEntityTypeAndEntityId(AvniEntityType.GeneralEncounter, uuid);
        if (errorRecord == null) {
            errorRecord = new ErrorRecord();
            errorRecord.setAvniEntityType(AvniEntityType.GeneralEncounter);
            errorRecord.setEntityId(uuid);
            errorRecord.addErrorLog(errorType, message, null);
            errorRecord.setProcessingDisabled(false);
            errorRecordRepository.saveErrorRecord(errorRecord);
            return;
        }
        Optional<ErrorRecordLog> sameType = errorRecord.getErrorRecordLogs().stream()
                .filter(log -> errorType.equals(log.getErrorType())).findFirst();
        if (sameType.isPresent()) {
            logger.info(String.format("Screening %s failed again with %s", uuid, errorTypeName));
            sameType.get().setErrorMsg(message);
            sameType.get().setLoggedAt(new Date());
        } else {
            errorRecord.addErrorLog(errorType, message, null);
        }
        errorRecordRepository.save(errorRecord);
    }
}
