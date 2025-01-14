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

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RwbUserNudgeService {

    private final IntegratingEntityStatusRepository integratingEntityStatusRepository;
    private final AvniRwbUserNudgeRepository avniRwbUserNudgeRepository;
    private CustomQueryRequest customQueryRequest;
    private int noOfDays;

    private static final Logger logger = Logger.getLogger(RwbUserNudgeService.class);

    @Autowired
    public RwbUserNudgeService(IntegratingEntityStatusRepository integratingEntityStatusRepository, AvniRwbUserNudgeRepository avniRwbUserNudgeRepository,
                               @Value("${rwb.avni.nudge.custom.query.name}") String customQueryName, @Value("${rwb.avni.nudge.no.of.days}") String numberOfDays) {
        this.integratingEntityStatusRepository = integratingEntityStatusRepository;
        this.avniRwbUserNudgeRepository = avniRwbUserNudgeRepository;
        this.customQueryRequest = new CustomQueryRequest(customQueryName, numberOfDays);
        this.noOfDays = Integer.parseInt(numberOfDays);
    }

    public List<NudgeUserRequestDTO> getUsersThatHaveToReceiveNudge() {
        CustomQueryResponse customQueryResponse = avniRwbUserNudgeRepository.executeCustomQuery(customQueryRequest);
        return customQueryResponse.getData().stream().map(row -> new NudgeUserRequestDTO(row.get(0).toString(), row.get(1).toString(),
                FormatAndParseUtil.toHumanReadableFormat(DateTime.now().minusDays(noOfDays).toDate()))).collect(Collectors.toList());
    }
    
    public SendMessageResponse nudgeUser(NudgeUserRequestDTO nudgeUserRequestDTO) {
        return avniRwbUserNudgeRepository.sendMessage(nudgeUserRequestDTO);
    }
}
