package org.avni_integration_service.rwb.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.CustomQueryRequest;
import org.avni_integration_service.avni.domain.CustomQueryResponse;
import org.avni_integration_service.avni.domain.SendMessageResponse;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.avni_integration_service.rwb.dto.NudgeUserRequestDTO;
import org.avni_integration_service.rwb.repository.AvniRwbUserNudgeRepository;
import org.avni_integration_service.util.FormatAndParseUtil;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RwbUserNudgeService {

    private final AvniRwbUserNudgeRepository avniRwbUserNudgeRepository;
    private CustomQueryRequest customQueryRequest;
    private int sinceNoOfDays;
    private int withinNoOfDays;

    private static final Logger logger = Logger.getLogger(RwbUserNudgeService.class);

    @Autowired
    public RwbUserNudgeService(AvniRwbUserNudgeRepository avniRwbUserNudgeRepository,
                               @Value("${rwb.avni.nudge.custom.query.name}") String customQueryName,
                               @Value("${rwb.avni.nudge.since.no.of.days}") int sinceNoOfDays,
                               @Value("${rwb.avni.nudge.within.no.of.days}") int withinNoOfDays) {
        this.avniRwbUserNudgeRepository = avniRwbUserNudgeRepository;
        this.customQueryRequest = new CustomQueryRequest(customQueryName, sinceNoOfDays);
        this.sinceNoOfDays = sinceNoOfDays;
        this.withinNoOfDays = withinNoOfDays;
    }

    public List<NudgeUserRequestDTO> getUsersThatHaveToReceiveNudge() {
        CustomQueryResponse customQueryResponse = avniRwbUserNudgeRepository.executeCustomQuery(customQueryRequest);
        logger.info(String.format("Custom Query returned %d number of users to nudge", customQueryResponse.getTotal()));
        return customQueryResponse.getData().stream().map(row -> new NudgeUserRequestDTO(row.get(0).toString(), row.get(1).toString(),
                String.valueOf(sinceNoOfDays), String.valueOf(withinNoOfDays))).collect(Collectors.toList());
    }
    
    public SendMessageResponse nudgeUser(NudgeUserRequestDTO nudgeUserRequestDTO) {
        return avniRwbUserNudgeRepository.sendMessage(nudgeUserRequestDTO);
    }
}
