package org.avni_integration_service.goonj.worker.goonj;

import org.apache.log4j.Logger;
import org.avni_integration_service.goonj.repository.GoonjBaseRepository;

import java.util.*;

public abstract class BaseGoonjWorker {
    private static final Logger logger = Logger.getLogger(BaseGoonjWorker.class);

    protected final GoonjBaseRepository crudRepository;
    protected final GoonjEventWorker eventWorker;

    public BaseGoonjWorker(GoonjBaseRepository crudRepository, GoonjEventWorker eventWorker) {
        this.crudRepository = crudRepository;
        this.eventWorker = eventWorker;
    }

    protected HashMap<String, Object>[] fetchEvents(Map<String, Object> filters) {
        HashMap<String, Object>[] events = crudRepository.fetchEvents(filters);
        if(events == null) {
            return new HashMap[0];
        }
        return events;
    }

    protected List<String> fetchDeletionEvents(Map<String, Object> filters) {
        List<String> deletionEvents = crudRepository.fetchDeletionEvents(filters);
        if(deletionEvents == null) {
            logger.info("No entities to delete");
            return new ArrayList<>();
        }
        logger.info(String.format("Deleting %d number of entities", deletionEvents.size()));
        return deletionEvents;
    }

    public void performAllProcesses() throws Exception {
        performAllProcesses(Collections.emptyMap(), true);
    }

    /**
     *
     * @param filters => {"state": "Karnataka", "account": "Goonj Karnataka", "dateTimestamp": "2024-10-10 12:34:56.123456Z"}
     * @param updateSyncStatus => Specify false for Adhoc tasks
     * @throws Exception
     */
    public void performAllProcesses(Map<String, Object> filters, boolean updateSyncStatus) throws Exception {
        processDeletions(filters);
        process(filters, updateSyncStatus);
    }

    public abstract void process(Map<String, Object> filters, boolean updateSyncStatus) throws Exception;
    public abstract void processDeletions(Map<String, Object> filters);
}
