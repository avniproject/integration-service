package org.avni_integration_service.scheduler;

import org.apache.log4j.Logger;
import org.avni_integration_service.goonj.GoonjAdhocTaskSatus;
import org.avni_integration_service.goonj.config.GoonjConfig;
import org.avni_integration_service.goonj.config.GoonjConstants;
import org.avni_integration_service.goonj.domain.GoonjAdhocTask;
import org.avni_integration_service.goonj.job.AvniGoonjAdhocJob;
import org.avni_integration_service.goonj.job.IntegrationTask;
import org.avni_integration_service.goonj.repository.GoonjAdhocTaskRepository;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.avni_integration_service.integration_data.repository.config.IntegrationSystemConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@EnableScheduling
public class AdhocTaskSchedulerService {

    private final Logger logger = Logger.getLogger(AdhocTaskSchedulerService.class);
    private final TaskScheduler taskScheduler;
    private final IntegrationSystemConfigRepository integrationSystemConfigRepository;
    private final IntegrationSystemRepository integrationSystemRepository;
    private final AvniGoonjAdhocJob avniGoonjAdhocJob;
    private final GoonjAdhocTaskRepository goonjAdhocTaskRepository;
    @Value("${avni.int.env:#{null}}")
    private String currentEnvironment;

    @Autowired
    public AdhocTaskSchedulerService(TaskScheduler taskScheduler, IntegrationSystemConfigRepository integrationSystemConfigRepository, IntegrationSystemRepository integrationSystemRepository, AvniGoonjAdhocJob avniGoonjAdhocJob, GoonjAdhocTaskRepository goonjAdhocTaskRepository) {
        this.taskScheduler = taskScheduler;
        this.integrationSystemConfigRepository = integrationSystemConfigRepository;
        this.integrationSystemRepository = integrationSystemRepository;
        this.avniGoonjAdhocJob = avniGoonjAdhocJob;
        this.goonjAdhocTaskRepository = goonjAdhocTaskRepository;
    }

    public void schedule(GoonjAdhocTask goonjAdhocTask, IntegrationSystem integrationSystem) {
        logger.info(String.format("scheduling start %s for %s (system: %s)", 
                goonjAdhocTask.getIntegrationTask(), goonjAdhocTask.getUuid(), integrationSystem.getName()));
        
        // Validate that the provided integration system is of type Goonj
        if (!IntegrationSystem.IntegrationSystemType.Goonj.equals(integrationSystem.getSystemType())) {
            logger.error(String.format("Goonj adhoc task SKIPPED: Invalid system type. Expected: Goonj, Actual: %s", 
                    integrationSystem.getSystemType()));
            goonjAdhocTask.setGoonjAdhocTaskSatus(GoonjAdhocTaskSatus.ERROR);
            goonjAdhocTaskRepository.save(goonjAdhocTask);
            return;
        }

        GoonjConfig goonjConfig = getGoonjConfig(integrationSystem);

        if (!isGoonjEnvironmentValid(goonjConfig)) {
            goonjAdhocTask.setGoonjAdhocTaskSatus(GoonjAdhocTaskSatus.ERROR);
            goonjAdhocTaskRepository.save(goonjAdhocTask);
            return;
        }

        IntegrationTask integrationTask = goonjAdhocTask.getIntegrationTask();
        Map<String, Object> taskConfig = goonjAdhocTask.getTaskConfig();
        Map<String, Object> filters = getFilters(goonjAdhocTask, taskConfig);
        taskScheduler.schedule(() -> {
            try {
                logger.info(String.format("running start for  %s (system: %s)", goonjAdhocTask, integrationSystem.getName()));
                avniGoonjAdhocJob.execute(goonjConfig, integrationTask, filters);
                goonjAdhocTask.setGoonjAdhocTaskSatus(GoonjAdhocTaskSatus.COMPLETED);
            } catch (Exception e) {
                logger.error(String.format("following exception comes during scheduling for %s", goonjAdhocTask.getUuid()), e);
                goonjAdhocTask.setGoonjAdhocTaskSatus(GoonjAdhocTaskSatus.ERROR);
            }
            goonjAdhocTaskRepository.save(goonjAdhocTask);
            logger.info(String.format("running end for %s ", goonjAdhocTask));
        }, goonjAdhocTask.getTriggerDateTime());
    }

    private GoonjConfig getGoonjConfig(IntegrationSystem integrationSystem) {
        IntegrationSystemConfigCollection integrationSystemConfigs = integrationSystemConfigRepository.getInstanceConfiguration(integrationSystem);
        return new GoonjConfig(integrationSystemConfigs, integrationSystem);
    }

    private Map<String,Object> getFilters(GoonjAdhocTask goonjAdhocTask, Map<String, Object> taskConfig) {
        Map<String,Object> filters = new HashMap<>();
        if(taskConfig !=null) {
            taskConfig.entrySet().stream().forEach(entry -> filters.put(entry.getKey(), entry.getValue()));
        }
        filters.put(GoonjConstants.FILTER_KEY_TIMESTAMP, goonjAdhocTask.getCutOffDateTime().toString());
        return filters;
    }

    private boolean isGoonjEnvironmentValid(GoonjConfig goonjConfig) {
        String dbEnvironment = goonjConfig.getEnvironment();

        if (currentEnvironment == null || currentEnvironment.isBlank()) {
            logger.warn("Goonj adhoc task SKIPPED: avni.int.env property not set. " +
                    "Set this property to match the DB config int_env to enable Goonj integration.");
            return false;
        }

        if (dbEnvironment == null || dbEnvironment.isBlank()) {
            logger.warn("Goonj adhoc task SKIPPED: int_env not configured in DB. " +
                    "Configure int_env in integration_system_config table to enable Goonj integration.");
            return false;
        }

        if (!currentEnvironment.equals(dbEnvironment)) {
            logger.error(String.format("Goonj adhoc task SKIPPED: Environment mismatch detected! " +
                            "Current environment (from env): '%s', DB config environment: '%s'. " +
                            "This likely indicates a non-prod environment using prod DB config.",
                    currentEnvironment, dbEnvironment));
            return false;
        }

        return true;
    }
}
