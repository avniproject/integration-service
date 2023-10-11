package org.avni_integration_service.lahi.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.integration_data.domain.AvniEntityType;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.error.ErrorRecord;
import org.avni_integration_service.integration_data.domain.error.ErrorType;
import org.avni_integration_service.integration_data.repository.ErrorRecordRepository;
import org.avni_integration_service.integration_data.repository.ErrorTypeRepository;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.avni_integration_service.lahi.domain.StudentErrorType;
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

    public void errorOccurred(String entityUuid, StudentErrorType studentErrorType, AvniEntityType avniEntityType, String errorMsg) {
        saveAvniError(entityUuid, studentErrorType, avniEntityType, errorMsg);
    }

    private void saveAvniError(String entityUuid, StudentErrorType studentErrorType, AvniEntityType avniEntityType, String errorMsg) {
        ErrorType errorType = getErrorType(studentErrorType);
        ErrorRecord errorRecord = new ErrorRecord();
        errorRecord.setIntegratingEntityType("Student");
        errorRecord.setIntegrationSystem(integrationSystemRepository.findBySystemType(IntegrationSystem.IntegrationSystemType.lahi));
        errorRecord.setEntityId(entityUuid);
        errorRecord.setProcessingDisabled(true);
        errorRecord.setAvniEntityType(avniEntityType);
        errorRecord.addErrorType(errorType,errorMsg);
        errorRecordRepository.save(errorRecord);
    }

    private ErrorType getErrorType(StudentErrorType studentErrorType) {
        String name = studentErrorType.name();
        IntegrationSystem  system = integrationSystemRepository.findBySystemType(IntegrationSystem.IntegrationSystemType.lahi);
        return errorTypeRepository.findByNameAndIntegrationSystem(name, system);
    }
}
