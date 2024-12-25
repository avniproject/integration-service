package org.avni_integration_service.rwb.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.CustomQueryRequest;
import org.avni_integration_service.avni.domain.CustomQueryResponse;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.avni_integration_service.rwb.repository.AvniRwbUserNudgeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RwbUserNudgeService {
    private static final String CUSTOM_QUERY_NAME = "Inactive users";
    private static final String USERNAME = "himeshr@rwbniti";

    private final IntegratingEntityStatusRepository integratingEntityStatusRepository;
    private final AvniRwbUserNudgeRepository avniRwbUserNudgeRepository;

    private static final Logger logger = Logger.getLogger(RwbUserNudgeService.class);
    private CustomQueryRequest customQueryRequest;

    public RwbUserNudgeService(IntegratingEntityStatusRepository integratingEntityStatusRepository, AvniRwbUserNudgeRepository avniRwbUserNudgeRepository) {
        this.integratingEntityStatusRepository = integratingEntityStatusRepository;
        this.avniRwbUserNudgeRepository = avniRwbUserNudgeRepository;
        this.customQueryRequest = new CustomQueryRequest(CUSTOM_QUERY_NAME, USERNAME);
    }

    public List<String> getUsersThatHaveToReceiveNudge() {
        //    TODO use BULK_FETCH_QUERY, 
        //     the query response has to be further filtered to send nudge only if not nudged in say past week or so
        CustomQueryResponse customQueryResponse = avniRwbUserNudgeRepository.executeCustomQuery(customQueryRequest);
        return customQueryResponse.getData().stream().map(row -> row.get(0).toString()).collect(Collectors.toList());
    }
    
    public void nudgeUser(String userId) {
        avniRwbUserNudgeRepository.sendMessage(userId);
    }
}
