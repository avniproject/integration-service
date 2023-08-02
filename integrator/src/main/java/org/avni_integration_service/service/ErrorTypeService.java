package org.avni_integration_service.service;

import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.error.ErrorType;
import org.avni_integration_service.integration_data.repository.ErrorTypeRepository;
import org.avni_integration_service.web.contract.ErrorTypeContract;
import org.springframework.stereotype.Service;

@Service
public class ErrorTypeService {
    private final ErrorTypeRepository errorTypeRepository;

    public ErrorTypeService(ErrorTypeRepository errorTypeRepository) {
        this.errorTypeRepository = errorTypeRepository;
    }

    public void createOrUpdateErrorType(ErrorTypeContract errorTypeContract, IntegrationSystem integrationSystem) {
        if (errorTypeContract.getUuid() == null) {
            throw new RuntimeException("ErrorType without uuid! " + errorTypeContract);
        }
        ErrorType errorType = errorTypeRepository.findByUuidAndIntegrationSystem(errorTypeContract.getUuid(), integrationSystem);
        if (errorType == null) {
            errorType = createErrorType(errorTypeContract);
        }
        this.updateAndSaveErrorType(errorType, errorTypeContract, integrationSystem);
    }

    public ErrorType createErrorType(ErrorTypeContract errorTypeContract) {
        ErrorType errorType = new ErrorType();
        errorType.setUuid(errorTypeContract.getUuid());
        return errorType;
    }

    public void updateAndSaveErrorType(ErrorType errorType, ErrorTypeContract errorTypeContract, IntegrationSystem integrationSystem) {
        errorType.setIntegrationSystem(integrationSystem);
        errorType.setVoided(errorTypeContract.isVoided());
        errorType.setName(errorTypeContract.getName());
        errorType.setComparisonOperator(errorTypeContract.getComparisonOperator());
        errorType.setComparisonValue(errorTypeContract.getComparisonValue());
        errorTypeRepository.save(errorType);
    }
}
