package org.avni_integration_service.goonj.job;

import com.bugsnag.Bugsnag;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.SyncDirection;
import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.goonj.config.GoonjAvniSessionFactory;
import org.avni_integration_service.goonj.config.GoonjConfig;
import org.avni_integration_service.goonj.config.GoonjContextProvider;
import org.avni_integration_service.goonj.worker.AvniGoonjErrorRecordsWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AvniGoonjFullErrorJob {
    private static final Logger logger = Logger.getLogger(AvniGoonjFullErrorJob.class);

    @Autowired
    private AvniGoonjErrorRecordsWorker avniGoonjErrorRecordsWorker;

    @Autowired
    private Bugsnag bugsnag;

    @Autowired
    private AvniHttpClient avniHttpClient;

    @Autowired
    private GoonjAvniSessionFactory goonjAvniSessionFactory;

    @Autowired
    private GoonjContextProvider goonjContextProvider;

    @Autowired
    private AvniGoonjErrorRecordsWorker errorRecordsWorker;

    public void execute(GoonjConfig goonjConfig) {
        logger.info("Executing Goonj Error Job");
        goonjContextProvider.set(goonjConfig);
        avniHttpClient.setAvniSession(goonjAvniSessionFactory.createSession());
        try {
            /**
             * All our Error Records for Goonj, i.e. Demand, Dispatch, Distro, DispatchReceipt and Activity
             * are stored using integrating_entity_type column, hence only SyncDirection.GoonjToAvni matters
             * and not the other-way(SyncDirection.AvniToGoonj) around.
             */
            avniGoonjErrorRecordsWorker.process(SyncDirection.GoonjToAvni, true);
        } catch (Exception e) {
            logger.error("Failed AvniGoonjFullErrorJob", e);
            bugsnag.notify(e);
        } finally {
            try {
                errorRecordsWorker.evaluateNewErrors();
            } catch (Exception e) {
                logger.error("Failed to evaluate new errors", e);
                bugsnag.notify(e);
            }
            AvniHttpClient.removeAvniSession();
            GoonjContextProvider.clear();
        }
    }
}
