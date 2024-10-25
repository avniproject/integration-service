package org.avni_integration_service.goonj.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.goonj.config.GoonjConstants;
import org.avni_integration_service.goonj.domain.GoonjAdhocTask;
import org.avni_integration_service.goonj.exceptions.GoonjAdhocException;
import org.avni_integration_service.goonj.job.IntegrationTask;
import org.avni_integration_service.goonj.worker.goonj.BaseGoonjWorker;
import org.avni_integration_service.goonj.worker.goonj.DemandWorker;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AdhocTaskSchedulerService {

    private  TaskScheduler taskScheduler; // TODO: 22/10/24  should be configurable. we have configuration in integrator but we can't use here
    private final Logger logger = Logger.getLogger(AdhocTaskSchedulerService.class);
    private final DemandWorker demandWorker;

    public AdhocTaskSchedulerService(DemandWorker demandWorker) {
        this.demandWorker = demandWorker;

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(3);
        scheduler.setThreadNamePrefix("SpringTaskScheduler-");
        scheduler.initialize();
        this.taskScheduler = scheduler;
    }


    public void schedule(GoonjAdhocTask goonjAdhocTask){
        logger.info(String.format("starting scheduling %s for %s",goonjAdhocTask.getIntegrationTask(),goonjAdhocTask.getUuid()));
        IntegrationTask integrationTask = goonjAdhocTask.getIntegrationTask();
        BaseGoonjWorker baseGoonjWorker = getBaseGoonjWorker(integrationTask);
        String cron = goonjAdhocTask.getCron();
        Map<String, String> taskConfig = goonjAdhocTask.getTaskConfig();
        Map<String,Object> filters = new HashMap<>();
        if(taskConfig!=null)
            taskConfig.entrySet().stream().forEach(entry-> filters.put(entry.getKey(), entry.getValue()));
        filters.put(GoonjConstants.FILTER_KEY_TIMESTAMP, goonjAdhocTask.getCutOffDateTime().toString());
        taskScheduler.schedule(() -> {
            try {
                baseGoonjWorker.performAllProcesses(filters);
            } catch (Exception e) {
                logger.error(String.format("following exception comes during scheduling for %s",goonjAdhocTask.getUuid()),e);
                throw new RuntimeException(e);
            }
        }, new CronTrigger(cron));
    }

    private BaseGoonjWorker getBaseGoonjWorker(IntegrationTask integrationTask){
        switch (integrationTask){
            case GoonjDemand -> {return this.demandWorker;}

//            case GoonjDispatch ->  applicationContext.getBean(DispatchWorker.class);
//            case GoonjInventory ->  applicationContext.getBean(InventoryWorker.class);
            default -> throw new GoonjAdhocException("Unable to find worker for task "+integrationTask, HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

}
