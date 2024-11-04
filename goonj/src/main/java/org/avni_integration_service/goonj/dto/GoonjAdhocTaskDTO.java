package org.avni_integration_service.goonj.dto;

import com.fasterxml.jackson.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GoonjAdhocTaskDTO {
    @NotBlank(message = "task can't be null or blank")
    private String task;
    private Map<String, String> taskConfig;
    @JsonFormat(shape = JsonFormat.Shape.STRING,pattern = "yyyy:MM:dd hh:mm:ss")
    @NotNull(message = "frequency can't be null or blank")
    private Date triggerDateTime;
    @JsonFormat(shape = JsonFormat.Shape.STRING,pattern = "yyyy:MM:dd hh:mm:ss")
    @NotNull(message = "cutoffdatetime can't be null or blank")
    private Date cutOffDateTime;
    private String uuid;
    @JsonProperty(value = "is_voided")
    private Boolean is_voided;

    public String getTask() {
        return task;
    }

    public void setTask(String string) {
        this.task = string;
    }

    public Map<String, String> getTaskConfig() {
        return taskConfig;
    }

    public void setTaskConfig(Map<String, String> taskConfig) {
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
