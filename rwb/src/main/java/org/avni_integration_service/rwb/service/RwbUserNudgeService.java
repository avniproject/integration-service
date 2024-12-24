package org.avni_integration_service.rwb.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.avni_integration_service.rwb.repository.AvniRwbUserNudgeRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class RwbUserNudgeService {
    private final IntegratingEntityStatusRepository integratingEntityStatusRepository;
    private final AvniRwbUserNudgeRepository avniRwbUserNudgeRepository;

    public static final String BULK_FETCH_QUERY = "<TODO>";

    private static final Logger logger = Logger.getLogger(RwbUserNudgeService.class);

    public RwbUserNudgeService(IntegratingEntityStatusRepository integratingEntityStatusRepository, AvniRwbUserNudgeRepository avniRwbUserNudgeRepository) {
        this.integratingEntityStatusRepository = integratingEntityStatusRepository;
        this.avniRwbUserNudgeRepository = avniRwbUserNudgeRepository;
    }

    public List<String> getUsersThatHaveToReceiveNudge() {
        //    TODO use BULK_FETCH_QUERY, 
        //     the query response has to be further filtered to send nudge only if not nudged in say past week or so
        return Arrays.asList("674");
    }
    
    public void nudgeUser(String userId) {
        avniRwbUserNudgeRepository.sendMessage(userId);
    }
}
