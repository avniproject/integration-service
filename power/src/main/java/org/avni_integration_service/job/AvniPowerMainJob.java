package org.avni_integration_service.job;

import com.bugsnag.Bugsnag;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.config.PowerAvniSessionFactory;
import org.avni_integration_service.util.HealthCheckService;
import org.avni_integration_service.worker.CallDetailsWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AvniPowerMainJob {

    private static final Logger logger = Logger.getLogger(AvniPowerMainJob.class);

    private static final String HEALTHCHECK_SLUG = "power";

    @Autowired
    private Bugsnag bugsnag;

    @Autowired
    private HealthCheckService healthCheckService;

    @Autowired
    private CallDetailsWorker callDetailsWorker;

    @Autowired
    PowerAvniSessionFactory powerAvniSessionFactory;

    @Autowired
    private AvniHttpClient avniHttpClient;

    public void execute() {
        try {
            avniHttpClient.setAvniSession(powerAvniSessionFactory.createSession());
            callDetailsWorker.fetchCallDetails();
            healthCheckService.success(HEALTHCHECK_SLUG);
        } catch (Throwable e) {
            healthCheckService.failure(HEALTHCHECK_SLUG);
            logger.error("Failed", e);
            bugsnag.notify(e);
        } finally {
            AvniHttpClient.removeAvniSession();
        }
    }
}
