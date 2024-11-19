package org.avni_integration_service.goonj.dto;

import com.fasterxml.jackson.annotation.*;
import org.avni_integration_service.goonj.job.IntegrationTask;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Map;

import static org.avni_integration_service.goonj.util.DateTimeUtil.IST;
import static org.avni_integration_service.goonj.util.DateTimeUtil.adhocTaskDateFormatDTOPattern;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GoonjAdhocTaskDTO {
    @NotBlank(message = "task can't be null or blank")
    private IntegrationTask task;
    private Map<String, Object> taskConfig;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = adhocTaskDateFormatDTOPattern, timezone = IST)
    @NotNull(message = "frequency can't be null or blank")
    private Date triggerDateTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = adhocTaskDateFormatDTOPattern, timezone = IST)
    @NotNull(message = "cutoffdatetime can't be null or blank")
    private Date cutOffDateTime;
    private String uuid;
    @JsonProperty(value = "is_voided")
    private Boolean is_voided;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String status;

    public IntegrationTask getTask() {
        return task;
    }

    public void setTask(IntegrationTask task) {
        this.task = task;
    }

    public Map<String, Object> getTaskConfig() {
        return taskConfig;
    }

    public void setTaskConfig(Map<String, Object> taskConfig) {
        this.taskConfig = taskConfig;
    }

    public Date getTriggerDateTime() {
        return triggerDateTime;
    }

    public void setTriggerDateTime(Date triggerDateTime) {
        this.triggerDateTime = triggerDateTime;
    }

    public Date getCutOffDateTime() {
        return cutOffDateTime;
    }

    public void setCutOffDateTime(Date cutOffDateTime) {
        this.cutOffDateTime = cutOffDateTime;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @JsonGetter(value = "is_voided")
    public Boolean isVoided() {
        return is_voided;
    }

    @JsonSetter(value = "is_voided")
    public void setIsVoided(Boolean is_voided) {
        this.is_voided = is_voided;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "GoonjAdhocTaskDTO{" +
                "task='" + task + '\'' +
                ", taskConfig=" + taskConfig +
                ", triggerDateTime='" + triggerDateTime + '\'' +
                ", cutOffDateTime=" + cutOffDateTime +
                ", uuid='" + uuid + '\'' +
                ", is_voided=" + is_voided +
                '}';
    }
}
