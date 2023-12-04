package org.avni_integration_service.lahi.job;

import com.bugsnag.Bugsnag;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.integration_data.context.ContextIntegrationSystem;
import org.avni_integration_service.integration_data.context.IntegrationContext;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
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
    private final IntegrationSystemRepository integrationSystemRepository;

    public AvniLahiMainJob(Bugsnag bugsnag, HealthCheckService healthCheckService,
                           LahiAvniSessionFactory lahiAvniSessionFactory, AvniHttpClient avniHttpClient,
                           StudentWorker studentWorker, IntegrationSystemRepository integrationSystemRepository) {
        this.bugsnag = bugsnag;
        this.healthCheckService = healthCheckService;
        this.lahiAvniSessionFactory = lahiAvniSessionFactory;
        this.avniHttpClient = avniHttpClient;
        this.studentWorker = studentWorker;
        this.integrationSystemRepository = integrationSystemRepository;
    }

    public void execute() {
        try {
            logger.info("Lahi Main Job Started");
            avniHttpClient.setAvniSession(lahiAvniSessionFactory.createSession());
            IntegrationContext.set(new ContextIntegrationSystem(integrationSystemRepository.findBySystemTypeAndName(IntegrationSystem.IntegrationSystemType.lahi, "lahi")));
            studentWorker.processStudents();
            healthCheckService.success(HEALTHCHECK_SLUG);
            logger.info("Lahi Main Job Ended");
        } catch (Exception e) {
            healthCheckService.failure(HEALTHCHECK_SLUG);
            logger.error("Failed", e);
            bugsnag.notify(e);
        }
    }
}
