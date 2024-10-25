package org.avni_integration_service.goonj.job;

import com.bugsnag.Bugsnag;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.goonj.config.GoonjAvniSessionFactory;
import org.avni_integration_service.goonj.config.GoonjConfig;
import org.avni_integration_service.goonj.config.GoonjContextProvider;
import org.avni_integration_service.goonj.worker.avni.ActivityWorker;
import org.avni_integration_service.goonj.worker.avni.DispatchReceiptWorker;
import org.avni_integration_service.goonj.worker.avni.DistributionWorker;
import org.avni_integration_service.goonj.worker.goonj.DemandWorker;
import org.avni_integration_service.goonj.worker.goonj.DispatchWorker;
import org.avni_integration_service.goonj.worker.goonj.InventoryWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AvniGoonjAdhocJob {
    private static final Logger logger = Logger.getLogger(AvniGoonjAdhocJob.class);

    @Autowired
    private DemandWorker demandWorker;

    @Autowired
    private DispatchWorker dispatchWorker;

    @Autowired
    private DispatchReceiptWorker dispatchReceiptWorker;

    @Autowired
    private DistributionWorker distributionWorker;

    @Autowired
    private ActivityWorker activityWorker;

    @Autowired
    private InventoryWorker inventoryWorker;

    @Autowired
    private Bugsnag bugsnag;

    @Autowired
    private AvniHttpClient avniHttpClient;

    @Autowired
    private GoonjAvniSessionFactory goonjAvniSessionFactory;

    @Autowired
    private GoonjContextProvider goonjContextProvider;

    public void execute(GoonjConfig goonjConfig, IntegrationTask integrationTask, Map<String, Object> filters) {
        try {
            logger.info("Executing Goonj Adhoc Job");
            goonjContextProvider.set(goonjConfig);
            avniHttpClient.setAvniSession(goonjAvniSessionFactory.createSession());

            List<IntegrationTask> tasks = IntegrationTask.getTasks(goonjConfig.getTasks());
            switch(integrationTask) {
                case GoonjDemand -> processDemand(tasks, filters);
                case GoonjDispatch -> processDispatch(tasks, filters);
                case GoonjInventory -> processInventory(tasks, filters);
                case AvniActivity -> processActivity(tasks, filters);
                case AvniDistribution -> processDistribution(tasks, filters);
                case AvniDispatchReceipt -> processDispatchReceipt(tasks, filters);
                case default -> {
                    return;
                }
            }
        } catch (Throwable e) {
            logger.error("Failed AvniGoonjAdhocJob", e);
            bugsnag.notify(e);
        } finally {
        }
    }

    private void processDemand(List<IntegrationTask> tasks, Map<String, Object> filters) {
        try {
            if (hasTask(tasks, IntegrationTask.GoonjDemand)) {
                logger.info("Processing GoonjDemand");
                /*
                  We are triggering deletion tagged along with Demand creations, as the Goonj System sends
                  the Deleted Demands info as part of the same getDemands API, but as a separate list,
                  without any TimeStamp and other minimal information details required to make an Update Subject as Voided call.
                  Therefore, we invoke the Delete API for subject using DemandId as externalId to mark a Demand as Voided.
                 */
                demandWorker.performAllProcesses(filters);
            }
        } catch (Throwable e) {
            logger.error("Failed processDemand", e);
            bugsnag.notify(e);
        }
    }

    private void processDispatch(List<IntegrationTask> tasks, Map<String, Object> filters) {
        try {
            if (hasTask(tasks, IntegrationTask.GoonjDispatch)) {
                logger.info("Processing GoonjDispatch");
                /*
                  We are triggering deletion tagged along with DispatchStatus creations, as the Goonj System sends
                  the Deleted DispatchStatuses info as part of the same getDispatchStatus API, but as a separate list,
                  without any TimeStamp and other minimal information details required to make an Update DispatchStatus as Voided call.
                  Therefore, we invoke the Delete API for DispatchStatus using DispatchStatusId as externalId to mark a DispatchStatus as Voided.
                 */
                dispatchWorker.performAllProcesses(filters);
            }
        } catch (Throwable e) {
            logger.error("Failed processDispatch", e);
            bugsnag.notify(e);
        }
    }

    private void processActivity(List<IntegrationTask> tasks, Map<String, Object> filters) {
        try {
            if (hasTask(tasks, IntegrationTask.AvniActivity)) {
                logger.info("Processing AvniActivity");
                activityWorker.performAllProcesses(filters);
            }
        } catch (Throwable e) {
            logger.error("Failed processActivity", e);
            bugsnag.notify(e);
        }
    }

    private void processDispatchReceipt(List<IntegrationTask> tasks, Map<String, Object> filters) {
        try {
            if (hasTask(tasks, IntegrationTask.AvniDispatchReceipt)) {
                logger.info("Processing AvniDispatchReceipt");
                dispatchReceiptWorker.performAllProcesses(filters);
            }
        } catch (Throwable e) {
            logger.error("Failed processDispatchReceipt", e);
            bugsnag.notify(e);
        }
    }

    private void processDistribution(List<IntegrationTask> tasks, Map<String, Object> filters) {
        try {
            if (hasTask(tasks, IntegrationTask.AvniDistribution)) {
                logger.info("Processing AvniDistribution");
                distributionWorker.performAllProcesses(filters);
            }
        } catch (Throwable e) {
            logger.error("Failed processDistribution", e);
            bugsnag.notify(e);
        }
    }

    private void processInventory(List<IntegrationTask> tasks, Map<String, Object> filters) {
        try {
            if (hasTask(tasks, IntegrationTask.GoonjInventory)) {
                logger.info("Processing GoonjInventory");
                inventoryWorker.performAllProcesses(filters);
            }
        } catch (Throwable e) {
            logger.error("Failed processInventory", e);
            bugsnag.notify(e);
        }
    }

    private boolean hasTask(List<IntegrationTask> tasks, IntegrationTask task) {
        return tasks.stream().filter(integrationTask -> integrationTask.equals(task)).findAny().orElse(null) != null;
    }
}
