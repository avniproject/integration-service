package org.avni_integration_service.scheduler;

import org.apache.log4j.Logger;
import org.avni_integration_service.goonj.config.GoonjConfig;
import org.avni_integration_service.goonj.config.GoonjConstants;
import org.avni_integration_service.goonj.domain.GoonjAdhocTask;
import org.avni_integration_service.goonj.job.AvniGoonjAdhocJob;
import org.avni_integration_service.goonj.job.IntegrationTask;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.avni_integration_service.integration_data.repository.config.IntegrationSystemConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public AdhocTaskSchedulerService(TaskScheduler taskScheduler, IntegrationSystemConfigRepository integrationSystemConfigRepository, IntegrationSystemRepository integrationSystemRepository, AvniGoonjAdhocJob avniGoonjAdhocJob) {
        this.taskScheduler = taskScheduler;
        this.integrationSystemConfigRepository = integrationSystemConfigRepository;
        this.integrationSystemRepository = integrationSystemRepository;
        this.avniGoonjAdhocJob = avniGoonjAdhocJob;
    }

    public void schedule(GoonjAdhocTask goonjAdhocTask){
        logger.info(String.format("starting scheduling %s for %s",goonjAdhocTask.getIntegrationTask(),goonjAdhocTask.getUuid()));
        IntegrationTask integrationTask = goonjAdhocTask.getIntegrationTask();
        Map<String, String> taskConfig = goonjAdhocTask.getTaskConfig();
        Map<String, Object> filters = getFilters(goonjAdhocTask, taskConfig);
        GoonjConfig goonjConfig = getGoonjConfig();
        taskScheduler.schedule(() -> {
            try {
                avniGoonjAdhocJob.execute(goonjConfig, integrationTask, filters);
            } catch (Exception e) {
                logger.error(String.format("following exception comes during scheduling for %s",goonjAdhocTask.getUuid()),e);
                throw new RuntimeException(e);
            }
        }, goonjAdhocTask.getTriggerDateTime());
    }

    private GoonjConfig getGoonjConfig() {
        List<IntegrationSystem> goonjSystems = integrationSystemRepository.findAllBySystemType(IntegrationSystem.IntegrationSystemType.Goonj);
        IntegrationSystem goonjSystem = goonjSystems.stream().findFirst().get();
        IntegrationSystemConfigCollection integrationSystemConfigs = integrationSystemConfigRepository.getInstanceConfiguration(goonjSystem);
        return new GoonjConfig(integrationSystemConfigs, goonjSystem);
    }

    private Map<String,Object> getFilters(GoonjAdhocTask goonjAdhocTask, Map<String, String> taskConfig) {
        Map<String,Object> filters = new HashMap<>();
        if(taskConfig !=null) {
            taskConfig.entrySet().stream().forEach(entry -> filters.put(entry.getKey(), entry.getValue()));
        }
        filters.put(GoonjConstants.FILTER_KEY_TIMESTAMP, goonjAdhocTask.getCutOffDateTime().toString());
        return filters;
    }

}
