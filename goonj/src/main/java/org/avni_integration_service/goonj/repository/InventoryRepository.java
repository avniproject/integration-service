package org.avni_integration_service.goonj.repository;

import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.goonj.GoonjEntityType;
import org.avni_integration_service.goonj.config.GoonjContextProvider;
import org.avni_integration_service.goonj.dto.InventoryResponseDTO;
import org.avni_integration_service.goonj.util.DateTimeUtil;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component("InventoryRepository")
public class InventoryRepository extends GoonjBaseRepository {

    public static final String GET_IMPLEMENTATION_INVENTORY_PATH = "ImplementationInventoryService/getImplementationInventories";
    public static final String FILTER_BY_DATE_TIME_PARAM = "dateTimeStamp";
    public static final String FILTER_BY_INVENTORY_ID = "inventoryId";

    @Autowired
    public InventoryRepository(IntegratingEntityStatusRepository integratingEntityStatusRepository, @Qualifier("GoonjRestTemplate") RestTemplate restTemplate, AvniHttpClient avniHttpClient, GoonjContextProvider goonjContextProvider) {
        super(integratingEntityStatusRepository, restTemplate, GoonjEntityType.Inventory.name(), avniHttpClient, goonjContextProvider);
    }

    @Override
    public HashMap<String, Object>[] fetchEvents(Map<String, Object> filters) {
        return getInventoryItemsDTOS(getCutOffDateTime(), filters).getInventoryItemsDTOS();
    }

    @Override
    public List<String> fetchDeletionEvents(Map<String, Object> filters) {
        return getInventoryItemsDTOS(getCutOffDateTime(), filters).getDeletedItemsDTOS();
    }

    @Override
    public HashMap<String, Object>[] createEvent(Subject subject) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HashMap<String, Object>[] createEvent(Subject subject, GeneralEncounter encounter) {
        throw new UnsupportedOperationException();
    }

    private InventoryResponseDTO getInventoryItemsDTOS(Date dateTime, Map<String, Object> filters) {
        return super.getResponse(GET_IMPLEMENTATION_INVENTORY_PATH, InventoryResponseDTO.class,
                getAPIFilters(FILTER_BY_DATE_TIME_PARAM, dateTime, filters));
    }

    public HashMap<String, Object> getInventoryItemsDTO(String uuid) {
        InventoryResponseDTO response = super.getSingleEntityResponse(GET_IMPLEMENTATION_INVENTORY_PATH, FILTER_BY_INVENTORY_ID, uuid, InventoryResponseDTO.class);
        return response.getInventoryItemsDTOS().length > 0 ? response.getInventoryItemsDTOS()[0] : null;
    }
}
