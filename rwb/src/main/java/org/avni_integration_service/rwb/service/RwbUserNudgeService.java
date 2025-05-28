package org.avni_integration_service.rwb.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.CustomQueryRequest;
import org.avni_integration_service.avni.domain.CustomQueryResponse;
import org.avni_integration_service.avni.domain.SendMessageResponse;
import org.avni_integration_service.rwb.config.RwbConfig;
import org.avni_integration_service.rwb.config.RwbContextProvider;
import org.avni_integration_service.rwb.dto.NudgeUserRequestDTO;
import org.avni_integration_service.rwb.repository.AvniRwbUserNudgeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RwbUserNudgeService {

    private static final Logger logger = Logger.getLogger(RwbUserNudgeService.class);
    private static final int USER_ID_RESULT_COL_INDEX = 0;
    private static final int USER_NAME_RESULT_COL_INDEX = 1;

    private final AvniRwbUserNudgeRepository avniRwbUserNudgeRepository;
    private final RwbContextProvider rwbContextProvider;

    @Autowired
    public RwbUserNudgeService(AvniRwbUserNudgeRepository avniRwbUserNudgeRepository,
                               RwbContextProvider rwbContextProvider) {
        this.avniRwbUserNudgeRepository = avniRwbUserNudgeRepository;
        this.rwbContextProvider = rwbContextProvider;
    }

    public List<NudgeUserRequestDTO> getUsersThatHaveToReceiveNudge() {
        RwbConfig rwbConfig = rwbContextProvider.get();
        CustomQueryRequest customQueryRequest = new CustomQueryRequest(rwbConfig.getCustomQueryName(), Integer.parseInt(rwbConfig.getSinceNoOfDays()));
        CustomQueryResponse customQueryResponse = avniRwbUserNudgeRepository.executeCustomQuery(customQueryRequest);
        logger.info(String.format("Custom Query returned %d number of users to nudge", customQueryResponse.getTotal()));
        return customQueryResponse.getData().stream().map(row ->
                new NudgeUserRequestDTO(row.get(USER_ID_RESULT_COL_INDEX).toString(), row.get(USER_NAME_RESULT_COL_INDEX).toString(),
                rwbConfig.getSinceNoOfDays(), rwbConfig.getWithinNoOfDays())).collect(Collectors.toList());
    }

    public SendMessageResponse nudgeUser(NudgeUserRequestDTO nudgeUserRequestDTO) {
        return avniRwbUserNudgeRepository.sendMessage(nudgeUserRequestDTO);
    }
}
