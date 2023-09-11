package org.avni_integration_service.lahi.job;

import com.bugsnag.Bugsnag;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.lahi.config.LahiAvniSessionFactory;
import org.avni_integration_service.lahi.worker.StudentWorker;
import org.avni_integration_service.util.HealthCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AvniLahiMainJob {

    private static final Logger logger = Logger.getLogger(AvniLahiMainJob.class);

    private static final String HEALTHCHECK_SLUG = "lahi";

    @Autowired
    private Bugsnag bugsnag;

    @Autowired
    private HealthCheckService healthCheckService;

    @Autowired
    LahiAvniSessionFactory lahiAvniSessionFactory;

    @Autowired
    private AvniHttpClient avniHttpClient;

    @Autowired
    private StudentWorker studentWorker;

    public void execute() {
        try {
            avniHttpClient.setAvniSession(lahiAvniSessionFactory.createSession());
            studentWorker.fetchDetails();
            // TODO: 08/09/23  
            healthCheckService.success(HEALTHCHECK_SLUG);
        } catch (Throwable e) {
            healthCheckService.failure(HEALTHCHECK_SLUG);
            logger.error("Failed", e);
            bugsnag.notify(e);
        }
    }
}
