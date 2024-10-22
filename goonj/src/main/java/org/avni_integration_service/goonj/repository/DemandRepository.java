package org.avni_integration_service.goonj.repository;

import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.goonj.GoonjEntityType;
import org.avni_integration_service.goonj.config.GoonjContextProvider;
import org.avni_integration_service.goonj.dto.DemandsResponseDTO;
import org.avni_integration_service.goonj.util.DateTimeUtil;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("DemandRepository")
public class DemandRepository extends GoonjBaseRepository {

    public static final String GET_DEMANDS_PATH = "DemandService/getDemands";
    public static final String DEMAND_ID = "demandId";
    public static final String FILTER_BY_DATE_TIME_PARAM = "dateTimestamp";

    @Autowired
    public DemandRepository(IntegratingEntityStatusRepository integratingEntityStatusRepository,
                            @Qualifier("GoonjRestTemplate")RestTemplate restTemplate,
                            AvniHttpClient avniHttpClient, GoonjContextProvider goonjContextProvider) {
        super(integratingEntityStatusRepository, restTemplate,
                GoonjEntityType.Demand.name(), avniHttpClient, goonjContextProvider);
    }

    @Override
    public HashMap<String, Object>[] fetchEvents(Map<String, Object> filters) {
        return getDemands(getCutOffDateTime(), filters).getDemands();
    }

    @Override
    public List<String> fetchDeletionEvents(Map<String, Object> filters) {
        return getDemands(getCutOffDateTime(), filters).getDeletedDemands();
    }

    @Override
    public HashMap<String, Object>[] createEvent(Subject subject) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HashMap<String, Object>[] createEvent(Subject subject, GeneralEncounter encounter) {
        throw new UnsupportedOperationException();
    }

    public DemandsResponseDTO getDemands(Date dateTime, Map<String, Object> filters) {
        return super.getResponse(GET_DEMANDS_PATH, DemandsResponseDTO.class,
                getAPIFilters(FILTER_BY_DATE_TIME_PARAM, dateTime, filters));
    }

    public HashMap<String, Object> getDemand(String uuid) {
        DemandsResponseDTO response = super.getSingleEntityResponse(GET_DEMANDS_PATH, DEMAND_ID, uuid, DemandsResponseDTO.class);
        return response.getDemands().length > 0 ? response.getDemands()[0] : null;
    }
}