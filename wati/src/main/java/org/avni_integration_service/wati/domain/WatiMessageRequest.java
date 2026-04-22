package org.avni_integration_service.wati.domain;

import org.avni_integration_service.integration_data.domain.framework.BaseIntegrationSpecificEntity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wati_message_request")
public class WatiMessageRequest extends BaseIntegrationSpecificEntity {

    @Column(nullable = false)
    private String flowName;

    @Column(nullable = false)
    private String entityId;

    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String templateName;

    @Column(columnDefinition = "jsonb")
    private String parameters;

    @Column
    private String locale;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private WatiMessageStatus status;

    @Column(nullable = false)
    private int attemptCount;

    @Column
    private LocalDateTime lastAttemptTime;

    @Column
    private LocalDateTime nextRetryTime;

    @Column
    private String watiMessageId;

    @Column
    private String watiStatus;

    @Column
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdDateTime;

    @Version
    @Column(nullable = false)
    private int version;

    public WatiMessageRequest() {
        super();
        this.status = WatiMessageStatus.Pending;
        this.attemptCount = 0;
        this.createdDateTime = LocalDateTime.now();
    }

    public String getFlowName() { return flowName; }
    public void setFlowName(String flowName) { this.flowName = flowName; }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public String getParameters() { return parameters; }
    public void setParameters(String parameters) { this.parameters = parameters; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    public WatiMessageStatus getStatus() { return status; }
    public void setStatus(WatiMessageStatus status) { this.status = status; }

    public int getAttemptCount() { return attemptCount; }
    public void setAttemptCount(int attemptCount) { this.attemptCount = attemptCount; }

    public LocalDateTime getLastAttemptTime() { return lastAttemptTime; }
    public void setLastAttemptTime(LocalDateTime lastAttemptTime) { this.lastAttemptTime = lastAttemptTime; }

    public LocalDateTime getNextRetryTime() { return nextRetryTime; }
    public void setNextRetryTime(LocalDateTime nextRetryTime) { this.nextRetryTime = nextRetryTime; }

    public String getWatiMessageId() { return watiMessageId; }
    public void setWatiMessageId(String watiMessageId) { this.watiMessageId = watiMessageId; }

    public String getWatiStatus() { return watiStatus; }
    public void setWatiStatus(String watiStatus) { this.watiStatus = watiStatus; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedDateTime() { return createdDateTime; }

    public int getVersion() { return version; }
}
