package org.avni_integration_service.tanuh.job;

import com.bugsnag.Bugsnag;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.integration_data.context.IntegrationContext;
import org.avni_integration_service.tanuh.config.TanuhAvniSessionFactory;
import org.avni_integration_service.tanuh.config.TanuhConfig;
import org.avni_integration_service.tanuh.config.TanuhContextProvider;
import org.avni_integration_service.util.HealthCheckService;
import org.springframework.stereotype.Component;

import static java.lang.String.format;

@Component
public class AvniTanuhErrorJob {
    private static final Logger logger = Logger.getLogger(AvniTanuhErrorJob.class);

    private final Bugsnag bugsnag;
    private final HealthCheckService healthCheckService;
    private final TanuhAvniSessionFactory tanuhAvniSessionFactory;
    private final AvniHttpClient avniHttpClient;
    private final TanuhContextProvider tanuhContextProvider;

    public AvniTanuhErrorJob(Bugsnag bugsnag, HealthCheckService healthCheckService,
                             TanuhAvniSessionFactory tanuhAvniSessionFactory, AvniHttpClient avniHttpClient,
                             TanuhContextProvider tanuhContextProvider) {
        this.bugsnag = bugsnag;
        this.healthCheckService = healthCheckService;
        this.tanuhAvniSessionFactory = tanuhAvniSessionFactory;
        this.avniHttpClient = avniHttpClient;
        this.tanuhContextProvider = tanuhContextProvider;
    }

    public void execute(TanuhConfig tanuhConfig) {
        String name = tanuhConfig.getIntegrationSystem().getName();
        try {
            logger.info(format("Tanuh Error Job Started: %s, as %s", name, tanuhConfig.getAvniImplUser()));
            tanuhContextProvider.set(tanuhConfig);
            avniHttpClient.setAvniSession(tanuhAvniSessionFactory.createSession());
            IntegrationContext.set(tanuhConfig.getIntegrationSystem());
            // Screenings waiting for a retry are scored here from integration-service#132 on.
            healthCheckService.success(tanuhConfig.getErrorJobHealthCheckSlug());
            logger.info(format("Tanuh Error Job Ended: %s", name));
        } catch (Exception e) {
            healthCheckService.failure(tanuhConfig.getErrorJobHealthCheckSlug());
            logger.error(format("Tanuh Error Job Errored: %s", name), e);
            bugsnag.notify(e);
        } finally {
            AvniHttpClient.removeAvniSession();
            TanuhContextProvider.clear();
            IntegrationContext.removeContext();
        }
    }
}
