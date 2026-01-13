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
import org.avni_integration_service.rwb.config.RwbConfig;
import org.avni_integration_service.rwb.job.AvniRwbMainJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import org.apache.log4j.Logger;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
@EnableScheduling
@ConditionalOnProperty(value = "avni.int.auto.close", havingValue = "false")
public class IntegrationJobScheduler {
    private static final Logger logger = Logger.getLogger(IntegrationJobScheduler.class);

    private final AvniGoonjMainJob avniGoonjMainJob;
    private final AvniGoonjFullErrorJob avniGoonjFullErrorJob;
    private final AvniPowerMainJob avniPowerMainJob;
    private final AvniPowerFullErrorJob avniPowerFullErrorJob;
    private final AvniLahiMainJob avniLahiMainJob;
    private final AvniLahiFullErrorJob avniLahiFullErrorJob;
    private final AvniRwbMainJob avniRwbMainJob;
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
    @Value("${avni.int.env:#{null}}")
    private String currentEnvironment;

    @Autowired
    public IntegrationJobScheduler(AvniGoonjMainJob avniGoonjMainJob, AvniGoonjFullErrorJob avniGoonjFullErrorJob,
                                   AvniPowerMainJob avniPowerMainJob, AvniPowerFullErrorJob avniPowerFullErrorJob,
                                   AvniLahiMainJob avniLahiMainJob, AvniLahiFullErrorJob avniLahiFullErrorJob,
                                   AvniAmritMainJob avniAmritMainJob, AvniAmritFullErrorJob avniAmritFullErrorJob,
                                   AvniRwbMainJob avniRwbMainJob, TaskScheduler taskScheduler,
                                   IntegrationSystemConfigRepository integrationSystemConfigRepository, IntegrationSystemRepository integrationSystemRepository) {
        this.avniGoonjMainJob = avniGoonjMainJob;
        this.avniGoonjFullErrorJob = avniGoonjFullErrorJob;
        this.avniPowerMainJob = avniPowerMainJob;
        this.avniPowerFullErrorJob = avniPowerFullErrorJob;
        this.avniRwbMainJob = avniRwbMainJob;
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
        logger.info("========== INTEGRATION SERVICE STARTUP - SCHEDULING JOBS ==========");
        List<String> activeModules = new ArrayList<>();
        List<String> skippedModules = new ArrayList<>();

        if (schedulePower()) activeModules.add("Power"); else skippedModules.add("Power");
        if (scheduleLahi()) activeModules.add("Lahi"); else skippedModules.add("Lahi");
        if (scheduleRwb()) activeModules.add("RWB"); else skippedModules.add("RWB");
        if (scheduleAmrit()) activeModules.add("Amrit"); else skippedModules.add("Amrit");
        if (scheduleGoonj()) activeModules.add("Goonj"); else skippedModules.add("Goonj");

        logStartupSummary(activeModules, skippedModules);
    }

    private void logStartupSummary(List<String> activeModules, List<String> skippedModules) {
        logger.info("========== INTEGRATION SERVICE STARTUP SUMMARY ==========");
        logger.info(String.format("Active modules (%d): %s", activeModules.size(),
                activeModules.isEmpty() ? "NONE" : String.join(", ", activeModules)));
        logger.info(String.format("Skipped modules (%d): %s", skippedModules.size(),
                skippedModules.isEmpty() ? "NONE" : String.join(", ", skippedModules)));
        logger.info("==========================================================");
    }

    private boolean scheduleGoonj() {
        logger.info("--- Goonj Module ---");
        List<IntegrationSystem> goonjSystems = integrationSystemRepository.findAllBySystemType(IntegrationSystem.IntegrationSystemType.Goonj);
        if (goonjSystems.isEmpty()) {
            logger.info("Goonj: No integration systems found in DB");
            return false;
        }

        boolean anyScheduled = false;
        for (IntegrationSystem goonjSystem : goonjSystems) {
            IntegrationSystemConfigCollection integrationSystemConfigs = integrationSystemConfigRepository.getInstanceConfiguration(goonjSystem);
            GoonjConfig goonjConfig = new GoonjConfig(integrationSystemConfigs, goonjSystem);

            if (!validateEnvironment(goonjSystem.getName(), goonjConfig.getEnvironment())) {
                continue;
            }

            String mainScheduledJobCron = integrationSystemConfigs.getMainScheduledJobCron();
            String errorScheduledJobCron = integrationSystemConfigs.getErrorScheduledJobCron();
            String tasks = goonjConfig.getTasks();

            logger.info(String.format("Goonj [%s]: Scheduling jobs - mainCron: %s, errorCron: %s, tasks: %s",
                    goonjSystem.getName(), mainScheduledJobCron, errorScheduledJobCron, tasks));

            if (CronExpression.isValidExpression(mainScheduledJobCron)) {
                taskScheduler.schedule(() -> avniGoonjMainJob.execute(goonjConfig), new CronTrigger(mainScheduledJobCron));
                logger.info(String.format("Goonj [%s]: Main job SCHEDULED with cron: %s", goonjSystem.getName(), mainScheduledJobCron));
                anyScheduled = true;
            } else {
                logger.info(String.format("Goonj [%s]: Main job SKIPPED - invalid cron: %s", goonjSystem.getName(), mainScheduledJobCron));
            }

            if (CronExpression.isValidExpression(errorScheduledJobCron)) {
                taskScheduler.schedule(() -> avniGoonjFullErrorJob.execute(goonjConfig), new CronTrigger(errorScheduledJobCron));
                logger.info(String.format("Goonj [%s]: Error job SCHEDULED with cron: %s", goonjSystem.getName(), errorScheduledJobCron));
                anyScheduled = true;
            } else {
                logger.info(String.format("Goonj [%s]: Error job SKIPPED - invalid cron: %s", goonjSystem.getName(), errorScheduledJobCron));
            }
        }
        return anyScheduled;
    }

    private boolean validateEnvironment(String systemName, String dbEnv) {
        if (currentEnvironment == null || currentEnvironment.isBlank()) {
            logger.warn(String.format("%s integration SKIPPED: avni.int.env property not set. " +
                    "Set this property to match the DB config int_env to enable integration.", systemName));
            return false;
        }

        if (dbEnv == null || dbEnv.isBlank()) {
            logger.warn(String.format("%s integration SKIPPED: int_env not configured in DB. " +
                    "Configure int_env in integration_system_config table to enable integration.", systemName));
            return false;
        }

        if (!currentEnvironment.equals(dbEnv)) {
            logger.error(String.format("%s integration SKIPPED: Environment mismatch detected! " +
                            "Current environment (from env): '%s', DB config environment: '%s'. " +
                            "This likely indicates a non-prod environment using prod DB config. " +
                            "Either update the avni.int.env property or clean up the DB integration config.",
                    systemName, currentEnvironment, dbEnv));
            return false;
        }

        logger.info(String.format("%s environment validation passed. Environment: %s", systemName, currentEnvironment));
        return true;
    }

    private boolean isRwbEnvironmentValid(RwbConfig rwbConfig) {
        return validateEnvironment(rwbConfig.getIntegrationSystem().getName(), rwbConfig.getEnvironment());
    }

    private boolean scheduleAmrit() {
        logger.info("--- Amrit Module ---");
        boolean scheduled = false;
        if (CronExpression.isValidExpression(amritCron)) {
            taskScheduler.schedule(avniAmritMainJob::execute, new CronTrigger(amritCron));
            logger.info(String.format("Amrit: Main job SCHEDULED with cron: %s", amritCron));
            scheduled = true;
        } else {
            logger.info(String.format("Amrit: Main job SKIPPED - invalid cron: %s", amritCron));
        }
        if (CronExpression.isValidExpression(amritCronError)) {
            taskScheduler.schedule(avniAmritFullErrorJob::execute, new CronTrigger(amritCronError));
            logger.info(String.format("Amrit: Error job SCHEDULED with cron: %s", amritCronError));
            scheduled = true;
        } else {
            logger.info(String.format("Amrit: Error job SKIPPED - invalid cron: %s", amritCronError));
        }
        return scheduled;
    }

    private boolean scheduleLahi() {
        logger.info("--- Lahi Module ---");
        boolean scheduled = false;
        if (CronExpression.isValidExpression(lahiCron)) {
            taskScheduler.schedule(avniLahiMainJob::execute, new CronTrigger(lahiCron));
            logger.info(String.format("Lahi: Main job SCHEDULED with cron: %s", lahiCron));
            scheduled = true;
        } else {
            logger.info(String.format("Lahi: Main job SKIPPED - invalid cron: %s", lahiCron));
        }
        if (CronExpression.isValidExpression(lahiCronError)) {
            taskScheduler.schedule(avniLahiFullErrorJob::execute, new CronTrigger(lahiCronError));
            logger.info(String.format("Lahi: Error job SCHEDULED with cron: %s", lahiCronError));
            scheduled = true;
        } else {
            logger.info(String.format("Lahi: Error job SKIPPED - invalid cron: %s", lahiCronError));
        }
        return scheduled;
    }

    private boolean schedulePower() {
        logger.info("--- Power Module ---");
        boolean scheduled = false;
        if (CronExpression.isValidExpression(powerCron)) {
            taskScheduler.schedule(avniPowerMainJob::execute, new CronTrigger(powerCron));
            logger.info(String.format("Power: Main job SCHEDULED with cron: %s", powerCron));
            scheduled = true;
        } else {
            logger.info(String.format("Power: Main job SKIPPED - invalid cron: %s", powerCron));
        }
        if (CronExpression.isValidExpression(powerCronError)) {
            taskScheduler.schedule(avniPowerFullErrorJob::execute, new CronTrigger(powerCronError));
            logger.info(String.format("Power: Error job SCHEDULED with cron: %s", powerCronError));
            scheduled = true;
        } else {
            logger.info(String.format("Power: Error job SKIPPED - invalid cron: %s", powerCronError));
        }
        return scheduled;
    }

    private boolean scheduleRwb() {
        logger.info("--- RWB Module ---");
        List<IntegrationSystem> rwbSystems = integrationSystemRepository.findAllBySystemType(IntegrationSystem.IntegrationSystemType.rwb);
        if (rwbSystems.isEmpty()) {
            logger.info("RWB: No integration systems found in DB");
            return false;
        }

        boolean anyScheduled = false;
        for (IntegrationSystem rwbSystem : rwbSystems) {
            IntegrationSystemConfigCollection integrationSystemConfigs = integrationSystemConfigRepository.getInstanceConfiguration(rwbSystem);
            RwbConfig rwbConfig = new RwbConfig(integrationSystemConfigs, rwbSystem);

            if (!isRwbEnvironmentValid(rwbConfig)) {
                continue;
            }

            String rwbCron = integrationSystemConfigs.getMainScheduledJobCron();

            if (CronExpression.isValidExpression(rwbCron)) {
                taskScheduler.schedule(() -> avniRwbMainJob.execute(rwbConfig), new CronTrigger(rwbCron));
                logger.info(String.format("RWB [%s]: Main job SCHEDULED with cron: %s", rwbSystem.getName(), rwbCron));
                anyScheduled = true;
            } else {
                logger.info(String.format("RWB [%s]: Main job SKIPPED - invalid cron: %s", rwbSystem.getName(), rwbCron));
            }
        }
        return anyScheduled;
    }
}
