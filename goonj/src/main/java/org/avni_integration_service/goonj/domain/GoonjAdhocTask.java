package org.avni_integration_service.goonj.domain;

import org.avni_integration_service.goonj.GoonjAdhocTaskSatus;
import org.avni_integration_service.goonj.converter.MapToJsonConverter;
import org.avni_integration_service.goonj.job.IntegrationTask;
import org.avni_integration_service.integration_data.domain.framework.AuditableEntity;
import org.hibernate.annotations.ColumnTransformer;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Map;

@Entity
@Table(name = "goonj_adhoc_task")
public class GoonjAdhocTask extends AuditableEntity {

    @Column(name = "integration_task")
    @Enumerated(EnumType.STRING)
    @NotNull
    private IntegrationTask integrationTask;

    @Column(name = "task_config",columnDefinition = "jsonb")
    @Convert(converter = MapToJsonConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    private Map<String,Object> taskConfig;

    @Column(name = "trigger_date_time")
    private Date triggerDateTime;

    @Column(name = "cut_off_date_time")
    private Date cutOffDateTime;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private GoonjAdhocTaskSatus goonjAdhocTaskSatus;

    public IntegrationTask getIntegrationTask() {
        return integrationTask;
    }

    public void setIntegrationTask(IntegrationTask integrationTask) {
        this.integrationTask = integrationTask;
    }

    public Map<String, Object> getTaskConfig() {
        return taskConfig;
    }

    public void setTaskConfig(Map<String,Object> taskConfig) {
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

    public GoonjAdhocTaskSatus getGoonjAdhocTaskSatus() {
        return goonjAdhocTaskSatus;
    }

    public void setGoonjAdhocTaskSatus(GoonjAdhocTaskSatus goonjAdhocTaskSatus) {
        this.goonjAdhocTaskSatus = goonjAdhocTaskSatus;
    }

    @Override
    public String toString() {
        return "GoonjAdhocTask{" +
                "integrationTask=" + integrationTask +
                ", taskConfig=" + taskConfig +
                ", triggerDateTime=" + triggerDateTime +
                ", cutOffDateTime=" + cutOffDateTime +
                ", goonjAdhocTaskSatus=" + goonjAdhocTaskSatus +
                '}';
    }
}
