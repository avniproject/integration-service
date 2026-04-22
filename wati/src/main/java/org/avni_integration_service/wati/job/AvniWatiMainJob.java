package org.avni_integration_service.wati.job;

import com.bugsnag.Bugsnag;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.wati.config.WatiAvniSessionFactory;
import org.avni_integration_service.wati.config.WatiConfig;
import org.avni_integration_service.wati.config.WatiContextProvider;
import org.avni_integration_service.wati.service.WatiMessageSendService;
import org.avni_integration_service.wati.worker.WatiFlowWorker;
import org.avni_integration_service.util.HealthCheckService;
import org.springframework.stereotype.Component;

import static java.lang.String.format;

@Component
public class AvniWatiMainJob {
    private static final String HEALTHCHECK_SLUG = "wati";
    private static final String EMPTY_STRING = "";
    private static final Logger logger = Logger.getLogger(AvniWatiMainJob.class);

    private final Bugsnag bugsnag;
    private final HealthCheckService healthCheckService;
    private final WatiAvniSessionFactory watiAvniSessionFactory;
    private final AvniHttpClient avniHttpClient;
    private final WatiContextProvider watiContextProvider;
    private final WatiFlowWorker watiFlowWorker;
    private final WatiMessageSendService watiMessageSendService;

    public AvniWatiMainJob(Bugsnag bugsnag, HealthCheckService healthCheckService,
                           WatiAvniSessionFactory watiAvniSessionFactory, AvniHttpClient avniHttpClient,
                           WatiContextProvider watiContextProvider, WatiFlowWorker watiFlowWorker,
                           WatiMessageSendService watiMessageSendService) {
        this.bugsnag = bugsnag;
        this.healthCheckService = healthCheckService;
        this.watiAvniSessionFactory = watiAvniSessionFactory;
        this.avniHttpClient = avniHttpClient;
        this.watiContextProvider = watiContextProvider;
        this.watiFlowWorker = watiFlowWorker;
        this.watiMessageSendService = watiMessageSendService;
    }

    public void execute(WatiConfig watiConfig) {
        String watiIntegrationSystemName = EMPTY_STRING;
        try {
            watiIntegrationSystemName = watiConfig.getIntegrationSystem().getName();
            logger.info(format("Wati Main Job Started: %s", watiIntegrationSystemName));
            watiContextProvider.set(watiConfig);
            avniHttpClient.setAvniSession(watiAvniSessionFactory.createSession());
            watiFlowWorker.processAllFlows();
            watiMessageSendService.sendPending();
            healthCheckService.success(HEALTHCHECK_SLUG);
            logger.info(format("Wati Main Job Ended: %s", watiIntegrationSystemName));
        } catch (Exception e) {
            healthCheckService.failure(HEALTHCHECK_SLUG);
            logger.error(format("Wati Main Job Errored: %s", watiIntegrationSystemName), e);
            bugsnag.notify(e);
        } finally {
            AvniHttpClient.removeAvniSession();
            WatiContextProvider.clear();
        }
    }
}
