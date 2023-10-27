package org.avni_integration_service.lahi.job;

import com.bugsnag.Bugsnag;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.lahi.config.LahiAvniSessionFactory;
import org.avni_integration_service.lahi.worker.StudentWorker;
import org.avni_integration_service.util.HealthCheckService;
import org.springframework.stereotype.Component;

@Component
public class AvniLahiMainJob {

    private static final Logger logger = Logger.getLogger(AvniLahiMainJob.class);

    private static final String HEALTHCHECK_SLUG = "lahi";

    private final Bugsnag bugsnag;

    private final HealthCheckService healthCheckService;

    private final LahiAvniSessionFactory lahiAvniSessionFactory;

    private final AvniHttpClient avniHttpClient;

    private final StudentWorker studentWorker;

    public AvniLahiMainJob(Bugsnag bugsnag, HealthCheckService healthCheckService,
                           LahiAvniSessionFactory lahiAvniSessionFactory, AvniHttpClient avniHttpClient,
                           StudentWorker studentWorker) {
        this.bugsnag = bugsnag;
        this.healthCheckService = healthCheckService;
        this.lahiAvniSessionFactory = lahiAvniSessionFactory;
        this.avniHttpClient = avniHttpClient;
        this.studentWorker = studentWorker;
    }

    public void execute() {
        try {
            logger.info("Lahi Main Job Started !!!!!");
            avniHttpClient.setAvniSession(lahiAvniSessionFactory.createSession());
            studentWorker.processStudent();
            // TODO: 08/09/23
            healthCheckService.success(HEALTHCHECK_SLUG);
            logger.info("Lahi Main Job Ended !!!!!");
        } catch (Throwable e) {
            healthCheckService.failure(HEALTHCHECK_SLUG);
            logger.error("Failed", e);
            bugsnag.notify(e);
        }
    }
}
