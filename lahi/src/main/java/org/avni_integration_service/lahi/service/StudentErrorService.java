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

    public void saveStudentError(Student lahiStudent, Throwable throwable, LahiErrorType lahiErrorType) {
        ErrorType errorType = getErrorType(lahiErrorType);
        ErrorRecord errorRecord = new ErrorRecord();
        errorRecord.setIntegratingEntityType("Student");
        errorRecord.setIntegrationSystem(integrationSystemRepository.find());
        errorRecord.setEntityId(lahiStudent.getFlowResultId());
        errorRecord.setAvniEntityType(AvniEntityType.Subject);
        errorRecord.setIntegratingEntityType(LahiEntityType.Student.name());
        errorRecord.addErrorLog(errorType, throwable.getMessage());
        errorRecord.setProcessingDisabled(false);
        errorRecordRepository.saveErrorRecord(errorRecord);
    }

    public void deleteStudentError(Student lahiStudent) {
        ErrorRecord errorRecord = errorRecordRepository.
                findByIntegratingEntityTypeAndEntityId(LahiEntityType.Student.name(), lahiStudent.getFlowResultId());
        if (errorRecord != null) {
            errorRecordRepository.delete(errorRecord);
        }
    }

    private ErrorType getErrorType(LahiErrorType errorType) {
        String name = errorType.name();
        return errorTypeRepository.findByNameAndIntegrationSystem(name, integrationSystemRepository.find());
    }
}
