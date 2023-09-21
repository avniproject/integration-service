package org.avni_integration_service.web.contract;

import org.avni_integration_service.integration_data.domain.error.ErrorType;
import org.avni_integration_service.integration_data.domain.error.ErrorTypeComparisonOperatorEnum;
import org.avni_integration_service.integration_data.domain.error.ErrorTypeFollowUpStep;
import org.avni_integration_service.integration_data.domain.framework.BaseEnum;
import org.avni_integration_service.integration_data.domain.framework.NamedEntity;

public class ErrorTypeContract extends NamedEntityContract{

    private ErrorTypeComparisonOperatorEnum comparisonOperator;
    private String comparisonValue;
    private ErrorTypeFollowUpStep followUpStep;

    public ErrorTypeContract() {
        this.followUpStep = ErrorTypeFollowUpStep.Process;
    }

    public ErrorTypeContract(BaseEnum baseEnum, ErrorTypeComparisonOperatorEnum comparisonOperator, String comparisonValue,
                             ErrorTypeFollowUpStep followUpStep ) {
        super(baseEnum);
        this.comparisonOperator = comparisonOperator;
        this.comparisonValue = comparisonValue;
        this.followUpStep = followUpStep;
    }

    public ErrorTypeContract(int id, String name, ErrorTypeComparisonOperatorEnum comparisonOperator, String comparisonValue,
                             ErrorTypeFollowUpStep followUpStep) {
        super(id, name);
        this.comparisonOperator = comparisonOperator;
        this.comparisonValue = comparisonValue;
        this.followUpStep = followUpStep;
    }

    public ErrorTypeContract(int id, String uuid, String name, ErrorTypeComparisonOperatorEnum comparisonOperator,
                             String comparisonValue, ErrorTypeFollowUpStep followUpStep) {
        super(id, uuid, name);
        this.comparisonOperator = comparisonOperator;
        this.comparisonValue = comparisonValue;
        this.followUpStep = followUpStep;
    }

    public ErrorTypeContract(NamedEntity namedEntity, ErrorTypeComparisonOperatorEnum comparisonOperator,
                             String comparisonValue, ErrorTypeFollowUpStep followUpStep) {
        super(namedEntity);
        this.comparisonOperator = comparisonOperator;
        this.comparisonValue = comparisonValue;
        this.followUpStep = followUpStep;
    }

    public ErrorTypeContract(ErrorType errorType) {
        this(errorType, errorType.getComparisonOperator(), errorType.getComparisonValue(), errorType.getFollowUpStep());
    }

    public ErrorTypeComparisonOperatorEnum getComparisonOperator() {
        return comparisonOperator;
    }

    public void setComparisonOperator(ErrorTypeComparisonOperatorEnum comparisonOperator) {
        this.comparisonOperator = comparisonOperator;
    }

    public String getComparisonValue() {
        return comparisonValue;
    }

    public void setComparisonValue(String comparisonValue) {
        this.comparisonValue = comparisonValue;
    }

    public ErrorTypeFollowUpStep getFollowUpStep() {
        return followUpStep;
    }

    public void setFollowUpStep(ErrorTypeFollowUpStep followUpStep) {
        this.followUpStep = followUpStep;
    }

    public static ErrorTypeContract fromErrorType(ErrorType errorType) {
        ErrorTypeContract errorTypeContract = new ErrorTypeContract();
        errorTypeContract.setUuid(errorType.getUuid());
        errorTypeContract.setName(errorType.getName());
        errorTypeContract.setComparisonOperator(errorType.getComparisonOperator());
        errorTypeContract.setComparisonValue(errorType.getComparisonValue());
        errorTypeContract.setFollowUpStep(errorType.getFollowUpStep());
        return errorTypeContract;
    }

}
