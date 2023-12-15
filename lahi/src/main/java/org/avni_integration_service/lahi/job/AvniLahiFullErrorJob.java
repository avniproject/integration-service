package org.avni_integration_service.lahi.job;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.integration_data.context.ContextIntegrationSystem;
import org.avni_integration_service.integration_data.context.IntegrationContext;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.avni_integration_service.lahi.config.LahiAvniSessionFactory;
import org.avni_integration_service.lahi.worker.StudentWorker;
import org.springframework.stereotype.Component;

@Component
public class AvniLahiFullErrorJob {
    private static final Logger logger = Logger.getLogger(AvniLahiFullErrorJob.class);

    private final LahiAvniSessionFactory lahiAvniSessionFactory;

    private final AvniHttpClient avniHttpClient;

    private final StudentWorker studentWorker;
    private final IntegrationSystemRepository integrationSystemRepository;

    public AvniLahiFullErrorJob(LahiAvniSessionFactory lahiAvniSessionFactory, AvniHttpClient avniHttpClient, StudentWorker studentWorker, IntegrationSystemRepository integrationSystemRepository) {
        this.lahiAvniSessionFactory = lahiAvniSessionFactory;
        this.avniHttpClient = avniHttpClient;
        this.studentWorker = studentWorker;
        this.integrationSystemRepository = integrationSystemRepository;
    }

    public void execute() {
        try {
            logger.info("Starting to process the error records");
            avniHttpClient.setAvniSession(lahiAvniSessionFactory.createSession());
            IntegrationContext.set(new ContextIntegrationSystem(integrationSystemRepository.findBySystemTypeAndName(IntegrationSystem.IntegrationSystemType.lahi, "lahi")));
            studentWorker.processErrors();
            logger.info("Lahi Error Job Ended");
        } catch (Exception e) {
            logger.error("Failed", e);
        }
    }
}
