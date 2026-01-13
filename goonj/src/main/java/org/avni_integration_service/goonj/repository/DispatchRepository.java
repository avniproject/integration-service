package org.avni_integration_service.goonj.repository;

import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.goonj.GoonjEntityType;
import org.avni_integration_service.goonj.config.GoonjContextProvider;
import org.avni_integration_service.goonj.dto.DeletedDispatchStatusLineItem;
import org.avni_integration_service.goonj.dto.DispatchesResponseDTO;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("DispatchRepository")
public class DispatchRepository extends GoonjBaseRepository {

    public static final String GET_DISPATCHES_PATH = "DispatchService/getDispatches";
    public static final String FILTER_BY_DATE_TIME_PARAM = "dateTimestamp";
    public static final String DISPATCH_STATUS_ID = "dispatchStatusId";

    @Autowired
    public DispatchRepository(IntegratingEntityStatusRepository integratingEntityStatusRepository,
                              @Qualifier("GoonjRestTemplate") RestTemplate restTemplate,
                              AvniHttpClient avniHttpClient, GoonjContextProvider goonjContextProvider) {
        super(integratingEntityStatusRepository, restTemplate, GoonjEntityType.Dispatch.name(), avniHttpClient, goonjContextProvider);
    }

    @Override
    public HashMap<String, Object>[] fetchEvents(Map<String, Object> filters) {
        return getDispatches(getCutOffDateTime(), filters).getDispatchStatuses();
    }

    @Override
    public List<String> fetchDeletionEvents(Map<String, Object> filters) {
        return getDispatches(getCutOffDateTime(), filters).getDeletedObjects().getDeletedDispatchStatuses();
    }

    public List<DeletedDispatchStatusLineItem> fetchDispatchLineItemDeletionEvents(Map<String, Object> filters) {
        return getDispatches(getCutOffDateTime(), filters).getDeletedObjects().getDeletedDispatchStatusLineItems();
    }

    public HashMap<String, Object>[] createEvent(Subject subject) {
        throw new UnsupportedOperationException();
    }
    @Override
    public HashMap<String, Object>[] createEvent(Subject subject, GeneralEncounter encounter) {
        throw new UnsupportedOperationException();
    }

    public DispatchesResponseDTO getDispatches(Date dateTime, Map<String, Object> filters) {
        return super.getResponse(GET_DISPATCHES_PATH, DispatchesResponseDTO.class,
                    getAPIFilters(FILTER_BY_DATE_TIME_PARAM, dateTime, filters));
    }

    public HashMap<String, Object> getDispatch(String uuid) {
        DispatchesResponseDTO response = super.getSingleEntityResponse(GET_DISPATCHES_PATH, DISPATCH_STATUS_ID, uuid, DispatchesResponseDTO.class);
        return response.getDispatchStatuses().length > 0 ? response.getDispatchStatuses()[0] : null;
    }
}
