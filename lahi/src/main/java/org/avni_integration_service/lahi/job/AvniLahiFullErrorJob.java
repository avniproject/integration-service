package org.avni_integration_service.lahi.job;

import com.bugsnag.Bugsnag;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.lahi.config.LahiAvniSessionFactory;
import org.avni_integration_service.lahi.worker.LahiErrorStudentWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AvniLahiFullErrorJob {
    private static final Logger logger = Logger.getLogger(AvniLahiFullErrorJob.class);

    @Autowired
    private Bugsnag bugsnag;

    @Autowired
    LahiAvniSessionFactory lahiAvniSessionFactory;

    @Autowired
    private AvniHttpClient avniHttpClient;

    @Autowired
    private LahiErrorStudentWorker lahiErrorStudentWorker;

    public void execute() {
        try {
            logger.info("Starting to process the error records for call details");
            avniHttpClient.setAvniSession(lahiAvniSessionFactory.createSession());
            lahiErrorStudentWorker.processErrors();
            // TODO: 08/09/23  
        } catch (Exception e) {
            logger.error("Failed", e);
            bugsnag.notify(e);
        }
    }

}