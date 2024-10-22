package org.avni_integration_service.goonj.job;

import com.bugsnag.Bugsnag;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.goonj.config.GoonjAvniSessionFactory;
import org.avni_integration_service.goonj.config.GoonjConfig;
import org.avni_integration_service.goonj.config.GoonjConstants;
import org.avni_integration_service.goonj.config.GoonjContextProvider;
import org.avni_integration_service.goonj.worker.AvniGoonjErrorRecordsWorker;
import org.avni_integration_service.goonj.worker.avni.ActivityWorker;
import org.avni_integration_service.goonj.worker.avni.DispatchReceiptWorker;
import org.avni_integration_service.goonj.worker.avni.DistributionWorker;
import org.avni_integration_service.goonj.worker.goonj.DemandWorker;
import org.avni_integration_service.goonj.worker.goonj.DispatchWorker;
import org.avni_integration_service.goonj.worker.goonj.InventoryWorker;
import org.avni_integration_service.integration_data.domain.error.ErrorTypeFollowUpStep;
import org.avni_integration_service.util.HealthCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class AvniGoonjMainJob {
    public static final long LONG_CONSTANT_ZERO = 0l;

    private static final Logger logger = Logger.getLogger(AvniGoonjMainJob.class);

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
    private AvniGoonjErrorRecordsWorker errorRecordsWorker;

    @Autowired
    private Bugsnag bugsnag;

    @Autowired
    private AvniHttpClient avniHttpClient;

    @Autowired
    private HealthCheckService healthCheckService;

    @Autowired
    private GoonjAvniSessionFactory goonjAvniSessionFactory;

    @Autowired
    private GoonjContextProvider goonjContextProvider;

    public void execute(GoonjConfig goonjConfig) {
        try {
            logger.info("Executing Goonj Main Job");
            goonjContextProvider.set(goonjConfig);
            avniHttpClient.setAvniSession(goonjAvniSessionFactory.createSession());

            List<IntegrationTask> tasks = IntegrationTask.getTasks(goonjConfig.getTasks());
            processDemandAndDispatch(tasks);
            processActivity(tasks);
            processDispatchReceiptAndDistribution(tasks);
            processInventory(tasks);
            healthCheckService.success(goonjConfig.getIntegrationSystem().getName().toLowerCase());
        } catch (Throwable e) {
            logger.error("Failed AvniGoonjMainJob", e);
            bugsnag.notify(e);
            healthCheckService.failure(goonjConfig.getIntegrationSystem().getName().toLowerCase());
        } finally {
            performAdditionalHealthChecks();
        }
    }

    private void performAdditionalHealthChecks() {
        try {
            Map<ErrorTypeFollowUpStep, Long> errorTypeFollowUpStepLongMap = errorRecordsWorker.evaluateNewErrors();
            pingGoonjInternalHealthCheckStatus(errorTypeFollowUpStepLongMap, ErrorTypeFollowUpStep.Internal, GoonjConstants.HEALTHCHECK_SLUG_GOONJ_INTEGRATION);
            pingGoonjInternalHealthCheckStatus(errorTypeFollowUpStepLongMap, ErrorTypeFollowUpStep.External, GoonjConstants.HEALTHCHECK_SLUG_GOONJ_SALESFORCE);
        } catch (Throwable e) {
            logger.error("Failed performing additional health checks", e);
            bugsnag.notify(e);
        }
    }

    private void pingGoonjInternalHealthCheckStatus(Map<ErrorTypeFollowUpStep, Long> errorTypeFollowUpStepLongMap, ErrorTypeFollowUpStep errorTypeFollowUpStep, String slug) {
        healthCheckService.ping(slug, errorTypeFollowUpStepLongMap.get(errorTypeFollowUpStep) > LONG_CONSTANT_ZERO ?
                        HealthCheckService.Status.FAILURE : HealthCheckService.Status.SUCCESS);
    }

    private void processDemandAndDispatch(List<IntegrationTask> tasks) {
        try {
            if (hasTask(tasks, IntegrationTask.GoonjDemand)) {
                logger.info("Processing GoonjDemand");
                /*
                  We are triggering deletion tagged along with Demand creations, as the Goonj System sends
                  the Deleted Demands info as part of the same getDemands API, but as a separate list,
                  without any TimeStamp and other minimal information details required to make an Update Subject as Voided call.
                  Therefore, we invoke the Delete API for subject using DemandId as externalId to mark a Demand as Voided.
                 */
                demandWorker.performAllProcesses();
            }
            if (hasTask(tasks, IntegrationTask.GoonjDispatch)) {
                logger.info("Processing GoonjDispatch");
                /*
                  We are triggering deletion tagged along with DispatchStatus creations, as the Goonj System sends
                  the Deleted DispatchStatuses info as part of the same getDispatchStatus API, but as a separate list,
                  without any TimeStamp and other minimal information details required to make an Update DispatchStatus as Voided call.
                  Therefore, we invoke the Delete API for DispatchStatus using DispatchStatusId as externalId to mark a DispatchStatus as Voided.
                 */
                dispatchWorker.performAllProcesses();
            }
        } catch (Throwable e) {
            logger.error("Failed processDemandAndDispatch", e);
            bugsnag.notify(e);
        }
    }

    private void processActivity(List<IntegrationTask> tasks) {
        try {
            if (hasTask(tasks, IntegrationTask.AvniActivity)) {
                logger.info("Processing AvniActivity");
                activityWorker.performAllProcesses();
            }
        } catch (Throwable e) {
            logger.error("Failed processActivity", e);
            bugsnag.notify(e);
        }
    }

    private void processDispatchReceiptAndDistribution(List<IntegrationTask> tasks) {
        try {
            if (hasTask(tasks, IntegrationTask.AvniDispatchReceipt)) {
                logger.info("Processing AvniDispatchReceipt");
                dispatchReceiptWorker.performAllProcesses();
            }
            if (hasTask(tasks, IntegrationTask.AvniDistribution)) {
                logger.info("Processing AvniDistribution");
                distributionWorker.performAllProcesses();
            }
        } catch (Throwable e) {
            logger.error("Failed processDispatchReceiptAndDistribution", e);
            bugsnag.notify(e);
        }
    }

    private void processInventory(List<IntegrationTask> tasks) {
        try {
            if (hasTask(tasks, IntegrationTask.GoonjInventory)) {
                logger.info("Processing GoonjInventory");
                inventoryWorker.performAllProcesses();
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
