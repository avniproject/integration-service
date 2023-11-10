package org.avni_integration_service.integration_data.domain.error;

import org.avni_integration_service.integration_data.domain.AvniEntityType;
import org.avni_integration_service.integration_data.domain.framework.BaseEntity;
import org.avni_integration_service.integration_data.domain.framework.BaseIntegrationSpecificEntity;
import org.springframework.util.StringUtils;

import javax.persistence.*;
import java.util.*;

@Entity
public class ErrorRecord extends BaseIntegrationSpecificEntity {
    private static final String EMPTY_STRING = "";
    @Column
    @Enumerated(EnumType.STRING)
    private AvniEntityType avniEntityType;

    @Column
    private String integratingEntityType;

    @Column
    private String entityId;

    @Column
    private boolean processingDisabled;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "errorRecord")
    private Set<ErrorRecordLog> errorRecordLogs = new HashSet<>();

    public AvniEntityType getAvniEntityType() {
        return avniEntityType;
    }

    public void setAvniEntityType(AvniEntityType avniEntityType) {
        this.avniEntityType = avniEntityType;
    }

    public String getIntegratingEntityType() {
        return integratingEntityType;
    }

    public void setIntegratingEntityType(String integratingEntityType) {
        this.integratingEntityType = integratingEntityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public boolean hasThisAsLastErrorTypeFollowUpStep(ErrorTypeFollowUpStep followUpStep) {
        ErrorRecordLog errorRecordLog = getLastErrorRecordLog();
        return errorRecordLog.getErrorType().getFollowUpStep().equals(followUpStep);
    }

    public boolean hasThisAsLastErrorType(ErrorType errorType) {
        return hasThisAsLastErrorTypeAndErrorMessage(errorType, null);
    }

    public boolean hasThisAsLastErrorTypeAndErrorMessage(ErrorType errorType, String errorMsg) {
        ErrorRecordLog errorRecordLog = getLastErrorRecordLog();
        boolean errorTypesAreSame = Objects.equals(errorRecordLog.getErrorType(), errorType);
        boolean errorMsgsAreSame = (StringUtils.hasText(errorMsg) && StringUtils.hasText(errorRecordLog.getErrorMsg()))
                    && errorMsg.equals(errorRecordLog.getErrorMsg());
        boolean errorMsgsAreNotPresent = (!StringUtils.hasText(errorMsg) && !StringUtils.hasText(errorRecordLog.getErrorMsg()));
        return errorTypesAreSame && (errorMsgsAreSame || errorMsgsAreNotPresent);
    }

    private ErrorRecordLog getLastErrorRecordLog() {
        return this.errorRecordLogs.stream().sorted(Comparator.comparing(BaseEntity::getId))
                .reduce((first, second) -> second).orElse(null);
    }

    public void addErrorLog(ErrorType errorType) {
        addErrorLog(errorType, EMPTY_STRING);
    }

    public void addErrorLog(ErrorType errorType, String errorMsg) {
        ErrorRecordLog errorRecordLog = new ErrorRecordLog();
        errorRecordLog.setErrorType(errorType);
        errorRecordLog.setErrorMsg(errorMsg);
        errorRecordLog.setLoggedAt(new Date());
        errorRecordLogs.add(errorRecordLog);
        errorRecordLog.setErrorRecord(this);
    }

    public boolean isProcessingDisabled() {
        return processingDisabled;
    }

    public void setProcessingDisabled(boolean processingDisabled) {
        this.processingDisabled = processingDisabled;
    }
}
