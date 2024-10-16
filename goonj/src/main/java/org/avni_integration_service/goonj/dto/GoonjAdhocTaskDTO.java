package org.avni_integration_service.goonj.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Date;
import java.util.Map;

public class GoonjAdhocTaskDTO {
    private String task;
    private Map<String, String> taskConfig;
    private int frequency;
    @JsonFormat(shape = JsonFormat.Shape.STRING,pattern = "yyyy:MM:dd hh:mm:ss")
    private Date cutoffdatetime;
    @JsonProperty(value = "isSuccessFull",access = JsonProperty.Access.READ_ONLY)
    private boolean isSuccessFull;

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

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public Date getCutoffdatetime() {
        return cutoffdatetime;
    }

    public void setCutoffdatetime(Date cutoffdatetime) {
        this.cutoffdatetime = cutoffdatetime;
    }

    @JsonGetter("isSuccessFull")
    public boolean isSuccessFull() {
        return isSuccessFull;
    }

    @JsonSetter("isSuccessFull")
    public void setSuccessFull(boolean isSuccessFull) {
        this.isSuccessFull = isSuccessFull;
    }

    @Override
    public String toString() {
        return "GoonjAdhocTaskDTO{" +
                "task='" + task + '\'' +
                ", taskConfig=" + taskConfig +
                ", frequency='" + frequency + '\'' +
                ", cutoffdatetime=" + cutoffdatetime +
                ", isSuccessFull=" + isSuccessFull +
                '}';
    }
}
