package org.avni_integration_service.lahi.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.integration_data.domain.AvniEntityType;
import org.avni_integration_service.integration_data.domain.error.ErrorRecord;
import org.avni_integration_service.integration_data.domain.error.ErrorType;
import org.avni_integration_service.integration_data.repository.ErrorRecordRepository;
import org.avni_integration_service.integration_data.repository.ErrorTypeRepository;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.avni_integration_service.lahi.config.LahiEntityType;
import org.avni_integration_service.lahi.config.LahiErrorType;
import org.avni_integration_service.lahi.domain.Student;
import org.springframework.stereotype.Service;

@Service
public class StudentErrorService {
    private static final Logger logger = Logger.getLogger(StudentErrorService.class);

    private final ErrorRecordRepository errorRecordRepository;
    private final IntegrationSystemRepository integrationSystemRepository;
    private final ErrorTypeRepository errorTypeRepository;

    public StudentErrorService(ErrorRecordRepository errorRecordRepository, IntegrationSystemRepository integrationSystemRepository, ErrorTypeRepository errorTypeRepository) {
        this.errorRecordRepository = errorRecordRepository;
        this.integrationSystemRepository = integrationSystemRepository;
        this.errorTypeRepository = errorTypeRepository;
    }

    public void platformError(Student lahiStudent, Throwable throwable) {
        saveStudentError(lahiStudent, throwable, LahiErrorType.PlatformError);
    }

    public void studentProcessingError(Student lahiStudent, Throwable throwable) {
        saveStudentError(lahiStudent, throwable, LahiErrorType.CommonError);
    }

    public ErrorRecord saveStudentError(Student lahiStudent, Throwable throwable, LahiErrorType lahiErrorType) {
        ErrorRecord errorRecord = getErrorRecord(lahiStudent);
        ErrorType errorType = getErrorType(lahiErrorType);
        String errorMsg = throwable.getMessage();
        String lahiEntityType = LahiEntityType.Student.name();
        String flowResultId = lahiStudent.getFlowResultId();
        if (errorRecord != null && errorRecord.hasThisAsLastErrorTypeAndErrorMessage(errorType, errorMsg)) {
            logger.info(String.format("Same error as the last processing for entity flowResultId %s, and type %s", flowResultId, lahiEntityType));
            if (!errorRecord.isProcessingDisabled()) {
                errorRecord.setProcessingDisabled(true);
                errorRecordRepository.save(errorRecord);
            }
            errorRecord.addErrorLog(errorType, errorMsg);
        } else if (errorRecord != null && !errorRecord.hasThisAsLastErrorTypeAndErrorMessage(errorType, errorMsg)) {
            logger.info(String.format("New error for entity flowResultId %s, and type %s", flowResultId, lahiEntityType));
            errorRecord.addErrorLog(errorType, errorMsg);
            errorRecordRepository.save(errorRecord);
        } else {
            errorRecord = new ErrorRecord();
            errorRecord.setIntegrationSystem(integrationSystemRepository.find());
            errorRecord.setEntityId(lahiStudent.getFlowResultId());
            errorRecord.setIntegratingEntityType(LahiEntityType.Student.name());
            errorRecord.addErrorLog(errorType, errorMsg);
            errorRecord.setProcessingDisabled(false);
            errorRecordRepository.save(errorRecord);
        }
        return errorRecord;
    }

    public void deleteStudentError(Student lahiStudent) {
        ErrorRecord errorRecord = getErrorRecord(lahiStudent);
        if (errorRecord != null) {
            errorRecordRepository.delete(errorRecord);
        }
    }

    private ErrorRecord getErrorRecord(Student lahiStudent) {
        ErrorRecord errorRecord = errorRecordRepository.
                findByIntegratingEntityTypeAndEntityId(LahiEntityType.Student.name(), lahiStudent.getFlowResultId());
        return errorRecord;
    }

    private ErrorType getErrorType(LahiErrorType errorType) {
        String name = errorType.name();
        return errorTypeRepository.findByNameAndIntegrationSystem(name, integrationSystemRepository.find());
    }
}
