package org.avni_integration_service.goonj.worker.goonj;

import org.apache.log4j.Logger;
import org.avni_integration_service.goonj.repository.GoonjBaseRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class BaseGoonjWorker {
    private static final Logger logger = Logger.getLogger(BaseGoonjWorker.class);

    protected final GoonjBaseRepository crudRepository;
    protected final GoonjEventWorker eventWorker;

    public BaseGoonjWorker(GoonjBaseRepository crudRepository, GoonjEventWorker eventWorker) {
        this.crudRepository = crudRepository;
        this.eventWorker = eventWorker;
    }

    protected HashMap<String, Object>[] fetchEvents() {
        HashMap<String, Object>[] events = crudRepository.fetchEvents();
        if(events == null) {
            return new HashMap[0];
        }
        return events;
    }

    protected List<String> fetchDeletionEvents() {
        List<String> deletionEvents = crudRepository.fetchDeletionEvents();
        if(deletionEvents == null) {
            logger.info("No entities to delete");
            return new ArrayList<>();
        }
        logger.info(String.format("Deleting %d number of entities", deletionEvents.size()));
        return deletionEvents;
    }

    public abstract void process() throws Exception;
    public abstract void processDeletions();
}
