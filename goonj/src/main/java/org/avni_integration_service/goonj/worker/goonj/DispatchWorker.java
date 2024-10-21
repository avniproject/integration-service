package org.avni_integration_service.goonj.worker.goonj;

import org.avni_integration_service.goonj.dto.DeletedDispatchStatusLineItem;
import org.avni_integration_service.goonj.repository.DispatchRepository;
import org.avni_integration_service.goonj.repository.GoonjBaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DispatchWorker extends BaseGoonjWorker {

    @Autowired
    public DispatchWorker(@Qualifier("DispatchRepository") GoonjBaseRepository crudRepository,
                        DispatchEventWorker dispatchEventWorker) {
        super(crudRepository, dispatchEventWorker);
    }

    @Override
    public void process(Map<String, Object> filters, boolean updateSyncStatus) throws Exception {
        HashMap<String, Object>[] dispatches = fetchEvents(filters);
        for (Map<String, Object> dispatch : dispatches) {
            eventWorker.process(dispatch, updateSyncStatus);
        }
    }

    @Override
    public void processDeletions(Map<String, Object> filters) {
        List<String> deletedDispatchStatuses = fetchDeletionEvents(filters);
        for (String deletedDS : deletedDispatchStatuses) {
            eventWorker.processDeletion(deletedDS);
        }
    }

    public void processDispatchLineItemDeletions(Map<String, Object> filters) {
        List<DeletedDispatchStatusLineItem> deletedDispatchStatusLineItems = ((DispatchRepository)crudRepository)
                .fetchDispatchLineItemDeletionEvents(filters);
        for (DeletedDispatchStatusLineItem deletedDSLI : deletedDispatchStatusLineItems) {
            ((DispatchEventWorker)eventWorker).processDispatchLineItemDeletion(deletedDSLI);
        }
    }

    @Override
    public void performAllProcesses() throws Exception {
        performAllProcesses(Collections.emptyMap(), true);
    }

    @Override
    public void performAllProcesses(Map<String, Object> filters, boolean updateSyncStatus) throws Exception {
        processDeletions(filters);
        processDispatchLineItemDeletions(filters);
        process(filters, updateSyncStatus);
    }
}
