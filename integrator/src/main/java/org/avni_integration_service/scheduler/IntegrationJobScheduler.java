package org.avni_integration_service.scheduler;

import org.avni_integration_service.amrit.job.AvniAmritFullErrorJob;
import org.avni_integration_service.amrit.job.AvniAmritMainJob;
import org.avni_integration_service.goonj.config.GoonjConfig;
import org.avni_integration_service.goonj.job.AvniGoonjFullErrorJob;
import org.avni_integration_service.goonj.job.AvniGoonjMainJob;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.avni_integration_service.integration_data.repository.config.IntegrationSystemConfigRepository;
import org.avni_integration_service.job.AvniPowerFullErrorJob;
import org.avni_integration_service.job.AvniPowerMainJob;
import org.avni_integration_service.lahi.job.AvniLahiFullErrorJob;
import org.avni_integration_service.lahi.job.AvniLahiMainJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

import static java.util.stream.Collectors.groupingBy;

@Component
@EnableScheduling
@ConditionalOnProperty(value = "avni.int.auto.close", havingValue = "false")
public class IntegrationJobScheduler {
    private final AvniGoonjMainJob avniGoonjMainJob;
    private final AvniGoonjFullErrorJob avniGoonjFullErrorJob;
    private final AvniPowerMainJob avniPowerMainJob;
    private final AvniPowerFullErrorJob avniPowerFullErrorJob;
    private final AvniLahiMainJob avniLahiMainJob;
    private final AvniLahiFullErrorJob avniLahiFullErrorJob;
    private final AvniAmritMainJob avniAmritMainJob;
    private final AvniAmritFullErrorJob avniAmritFullErrorJob;
    private final TaskScheduler taskScheduler;
    private final IntegrationSystemConfigRepository integrationSystemConfigRepository;
    private final IntegrationSystemRepository integrationSystemRepository;

    @Value("${power.app.cron.main}")
    private String powerCron;
    @Value("${power.app.cron.full.error}")
    private String powerCronError;
    @Value("${lahi.app.cron.main}")
    private String lahiCron;
    @Value("${lahi.app.cron.full.error}")
    private String lahiCronError;
    @Value("${amrit.app.cron.main}")
    private String amritCron;
    @Value("${amrit.app.cron.full.error}")
    private String amritCronError;

    @Autowired
    public IntegrationJobScheduler(AvniGoonjMainJob avniGoonjMainJob, AvniGoonjFullErrorJob avniGoonjFullErrorJob,
                                   AvniPowerMainJob avniPowerMainJob, AvniPowerFullErrorJob avniPowerFullErrorJob,
                                   AvniLahiMainJob avniLahiMainJob, AvniLahiFullErrorJob avniLahiFullErrorJob,
                                   AvniAmritMainJob avniAmritMainJob, AvniAmritFullErrorJob avniAmritFullErrorJob, TaskScheduler taskScheduler, IntegrationSystemConfigRepository integrationSystemConfigRepository, IntegrationSystemRepository integrationSystemRepository) {
        this.avniGoonjMainJob = avniGoonjMainJob;
        this.avniGoonjFullErrorJob = avniGoonjFullErrorJob;
        this.avniPowerMainJob = avniPowerMainJob;
        this.avniPowerFullErrorJob = avniPowerFullErrorJob;
        this.avniLahiMainJob = avniLahiMainJob;
        this.avniLahiFullErrorJob = avniLahiFullErrorJob;
        this.avniAmritMainJob = avniAmritMainJob;
        this.avniAmritFullErrorJob = avniAmritFullErrorJob;
        this.taskScheduler = taskScheduler;
        this.integrationSystemConfigRepository = integrationSystemConfigRepository;
        this.integrationSystemRepository = integrationSystemRepository;
    }

    @PostConstruct
    public void scheduleAll() {
        if (CronExpression.isValidExpression(powerCron)) taskScheduler.schedule(avniPowerMainJob::execute, new CronTrigger(powerCron));
        if (CronExpression.isValidExpression(powerCronError)) taskScheduler.schedule(avniPowerFullErrorJob::execute, new CronTrigger(powerCronError));
        if (CronExpression.isValidExpression(lahiCron)) taskScheduler.schedule(avniLahiMainJob::execute, new CronTrigger(lahiCron));
        if (CronExpression.isValidExpression(lahiCronError)) taskScheduler.schedule(avniLahiFullErrorJob::execute, new CronTrigger(lahiCronError));
        if (CronExpression.isValidExpression(amritCron)) taskScheduler.schedule(avniAmritMainJob::execute, new CronTrigger(amritCron));
        if (CronExpression.isValidExpression(amritCronError)) taskScheduler.schedule(avniAmritFullErrorJob::execute, new CronTrigger(amritCronError));

        List<IntegrationSystem> goonjSystems = integrationSystemRepository.findAllBySystemType(IntegrationSystem.IntegrationSystemType.Goonj);
        goonjSystems.forEach(goonjSystem -> {
            IntegrationSystemConfigCollection integrationSystemConfigs = integrationSystemConfigRepository.getInstanceConfiguration(goonjSystem);
            GoonjConfig goonjConfig = new GoonjConfig(integrationSystemConfigs, goonjSystem);
            String mainScheduledJobCron = integrationSystemConfigs.getMainScheduledJobCron();
            String errorScheduledJobCron = integrationSystemConfigs.getErrorScheduledJobCron();

            if (CronExpression.isValidExpression(mainScheduledJobCron))
                taskScheduler.schedule(() -> avniGoonjMainJob.execute(goonjConfig), new CronTrigger(mainScheduledJobCron));

            if (CronExpression.isValidExpression(errorScheduledJobCron))
                taskScheduler.schedule(() -> avniGoonjFullErrorJob.execute(goonjConfig), new CronTrigger(errorScheduledJobCron));
        });
    }
}
