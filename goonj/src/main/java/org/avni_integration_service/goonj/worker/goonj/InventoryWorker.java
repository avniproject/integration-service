package org.avni_integration_service.goonj.worker.goonj;

import org.avni_integration_service.goonj.repository.GoonjBaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class InventoryWorker extends BaseGoonjWorker {
    @Autowired
    public InventoryWorker(@Qualifier("InventoryRepository") GoonjBaseRepository crudRepository,
                           InventoryEventWorker inventoryEventWorker) {
        super(crudRepository, inventoryEventWorker);
    }

    @Override
    public void process(Map<String, Object> filters, boolean updateSyncStatus) throws Exception {
        HashMap<String, Object>[] inventoryItems = fetchEvents(filters);
        for (Map<String, Object> items : inventoryItems) {
            eventWorker.process(items, updateSyncStatus);
        }
    }

    @Override
    public void processDeletions(Map<String, Object> filters) {
        List<String> deletedItems = fetchDeletionEvents(filters);
        for (String deletedDS : deletedItems) {
            eventWorker.processDeletion(deletedDS);
        }
    }
}
