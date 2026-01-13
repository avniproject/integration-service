package org.avni_integration_service.lahi.service;

import org.apache.log4j.Logger;
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

    public void saveStudentError(Student lahiStudent, Exception exception) {
        ErrorRecord errorRecord = getErrorRecord(lahiStudent);
        saveStudentError(lahiStudent, exception, errorRecord);
    }

    public void saveStudentError(Student lahiStudent, Exception exception, ErrorRecord errorRecord) {
        ErrorType errorType = getErrorType(LahiErrorType.getErrorType(exception));
        String errorMsg = exception.getMessage();
        String lahiEntityType = LahiEntityType.Student.name();
        String flowResultId = lahiStudent.getFlowResultId();
        if (errorRecord != null) {
            if (errorRecord.hasThisAsLastErrorTypeAndErrorMessage(errorType, errorMsg))
                logger.info(String.format("Same error as the last processing for entity flowResultId %s, and type %s", flowResultId, lahiEntityType));
            else
                errorRecord.addErrorLog(errorType, errorMsg);
        } else {
            errorRecord = new ErrorRecord();
            errorRecord.setIntegrationSystem(integrationSystemRepository.find());
            errorRecord.setEntityId(lahiStudent.getFlowResultId());
            errorRecord.setIntegratingEntityType(LahiEntityType.Student.name());
            errorRecord.addErrorLog(errorType, errorMsg);
            errorRecord.setProcessingDisabled(false);
        }
        errorRecordRepository.save(errorRecord);
    }

    private ErrorRecord getErrorRecord(Student lahiStudent) {
        return errorRecordRepository.
                findByIntegratingEntityTypeAndEntityId(LahiEntityType.Student.name(), lahiStudent.getFlowResultId());
    }

    private ErrorType getErrorType(LahiErrorType errorType) {
        String name = errorType.name();
        return errorTypeRepository.findByNameAndIntegrationSystem(name, integrationSystemRepository.find());
    }

    public void processed(ErrorRecord errorRecord, boolean success) {
        if (success)
            errorRecordRepository.delete(errorRecord);
        else {
            errorRecord.setProcessingDisabled(true);
            errorRecordRepository.saveErrorRecord(errorRecord);
        }
    }
}
