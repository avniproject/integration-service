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

    public void studentProcessingError(Student lahiStudent, Throwable throwable) {
        ErrorType errorType = getErrorType(LahiErrorType.CommonError);
        ErrorRecord errorRecord = new ErrorRecord();
        errorRecord.setIntegratingEntityType("Student");
        errorRecord.setIntegrationSystem(integrationSystemRepository.find());
        errorRecord.setEntityId(lahiStudent.getFlowResultId());
        errorRecord.setProcessingDisabled(false);
        errorRecord.setAvniEntityType(AvniEntityType.Subject);
        errorRecord.setIntegratingEntityType(LahiEntityType.Student.name());
        errorRecord.addErrorType(errorType, throwable.getMessage());
        errorRecordRepository.saveErrorRecord(errorRecord);
    }

    private ErrorType getErrorType(LahiErrorType errorType) {
        String name = errorType.name();
        return errorTypeRepository.findByNameAndIntegrationSystem(name, integrationSystemRepository.find());
    }
}
