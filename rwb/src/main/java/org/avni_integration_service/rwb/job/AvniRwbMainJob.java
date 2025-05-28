package org.avni_integration_service.rwb.job;

import com.bugsnag.Bugsnag;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.rwb.config.RwbAvniSessionFactory;
import org.avni_integration_service.rwb.config.RwbConfig;
import org.avni_integration_service.rwb.config.RwbContextProvider;
import org.avni_integration_service.rwb.worker.RWBUsersNudgeWorker;
import org.avni_integration_service.util.HealthCheckService;
import org.springframework.stereotype.Component;

import static java.lang.String.format;

@Component
public class AvniRwbMainJob {
    private static final String HEALTHCHECK_SLUG = "rwb";
    private static final String EMPTY_STRING = "";
    private static final Logger logger = Logger.getLogger(AvniRwbMainJob.class);

    private final Bugsnag bugsnag;
    private final HealthCheckService healthCheckService;
    private final RwbAvniSessionFactory rwbAvniSessionFactory;
    private final AvniHttpClient avniHttpClient;
    private final RwbContextProvider rwbContextProvider;
    private final RWBUsersNudgeWorker rwbUsersNudgeWorker;

    public AvniRwbMainJob(Bugsnag bugsnag, HealthCheckService healthCheckService,
                          RwbAvniSessionFactory rwbAvniSessionFactory, AvniHttpClient avniHttpClient,
                          RwbContextProvider rwbContextProvider, RWBUsersNudgeWorker rwbUsersNudgeWorker) {
        this.bugsnag = bugsnag;
        this.healthCheckService = healthCheckService;
        this.rwbAvniSessionFactory = rwbAvniSessionFactory;
        this.avniHttpClient = avniHttpClient;
        this.rwbUsersNudgeWorker = rwbUsersNudgeWorker;
        this.rwbContextProvider = rwbContextProvider;
    }

    public void execute(RwbConfig rwbConfig) {
        String rwbIntegrationSystemName = EMPTY_STRING;
        try {
            rwbIntegrationSystemName = rwbConfig.getIntegrationSystem().getName();
            logger.info(format("Rwb Main Job Started: %s", rwbIntegrationSystemName));
            rwbContextProvider.set(rwbConfig);
            avniHttpClient.setAvniSession(rwbAvniSessionFactory.createSession());
            rwbUsersNudgeWorker.processUsers();
            healthCheckService.success(HEALTHCHECK_SLUG);
            logger.info(format("Rwb Main Job Ended: %s", rwbIntegrationSystemName));
        } catch (Exception e) {
            healthCheckService.failure(HEALTHCHECK_SLUG);
            logger.error(format("Rwb Main Job Errored: %s", rwbIntegrationSystemName), e);
            bugsnag.notify(e);
        } finally {
            AvniHttpClient.removeAvniSession();
            RwbContextProvider.clear();
        }
    }
}
