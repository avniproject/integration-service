package org.avni_integration_service.goonj.worker.goonj;

import org.avni_integration_service.goonj.repository.GoonjBaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DemandWorker extends BaseGoonjWorker {
    @Autowired
    public DemandWorker(@Qualifier("DemandRepository") GoonjBaseRepository crudRepository,
                        DemandEventWorker demandEventWorker) {
        super(crudRepository, demandEventWorker);
    }

    @Override
    public void process(Map<String, Object> filters, boolean updateSyncStatus) throws Exception {
        HashMap<String, Object>[] demands = fetchEvents(filters);
        for (Map<String, Object> demand : demands) {
            eventWorker.process(demand, updateSyncStatus);
        }
    }

    @Override
    public void processDeletions(Map<String, Object> filters) {
        List<String> deletedDemands = fetchDeletionEvents(filters);
        for (String deletedDemand : deletedDemands) {
            eventWorker.processDeletion(deletedDemand);
        }
    }
}
