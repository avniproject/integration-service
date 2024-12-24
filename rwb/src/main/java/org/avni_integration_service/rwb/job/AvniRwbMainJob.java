package org.avni_integration_service.rwb.job;

import com.bugsnag.Bugsnag;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.integration_data.context.ContextIntegrationSystem;
import org.avni_integration_service.integration_data.context.IntegrationContext;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.avni_integration_service.rwb.config.RwbAvniSessionFactory;
import org.avni_integration_service.rwb.worker.RWBUsersNudgeWorker;
import org.avni_integration_service.util.HealthCheckService;
import org.springframework.stereotype.Component;

@Component
public class AvniRwbMainJob {

    private static final Logger logger = Logger.getLogger(AvniRwbMainJob.class);

    private static final String RWB_HEALTHCHECK_SLUG = "rwb";

    private final Bugsnag bugsnag;

    private final HealthCheckService healthCheckService;

    private final RwbAvniSessionFactory rwbAvniSessionFactory;

    private final AvniHttpClient avniHttpClient;

    private final RWBUsersNudgeWorker rwbUsersNudgeWorker;
    private final IntegrationSystemRepository integrationSystemRepository;

    public AvniRwbMainJob(Bugsnag bugsnag, HealthCheckService healthCheckService,
                          RwbAvniSessionFactory rwbAvniSessionFactory, AvniHttpClient avniHttpClient,
                          RWBUsersNudgeWorker rwbUsersNudgeWorker, IntegrationSystemRepository integrationSystemRepository) {
        this.bugsnag = bugsnag;
        this.healthCheckService = healthCheckService;
        this.rwbAvniSessionFactory = rwbAvniSessionFactory;
        this.avniHttpClient = avniHttpClient;
        this.rwbUsersNudgeWorker = rwbUsersNudgeWorker;
        this.integrationSystemRepository = integrationSystemRepository;
    }

    public void execute() {
        try {
            logger.info("Rwb Main Job Started");
            avniHttpClient.setAvniSession(rwbAvniSessionFactory.createSession());
            IntegrationContext.set(new ContextIntegrationSystem(integrationSystemRepository.findBySystemTypeAndName(IntegrationSystem.IntegrationSystemType.rwb, "rwb")));
            rwbUsersNudgeWorker.processUsers();
            healthCheckService.success(RWB_HEALTHCHECK_SLUG);
            logger.info("Rwb Main Job Ended");
        } catch (Exception e) {
            healthCheckService.failure(RWB_HEALTHCHECK_SLUG);
            logger.error("Failed", e);
            bugsnag.notify(e);
        }
    }
}
