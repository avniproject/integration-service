package org.avni_integration_service.rwb.worker;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.SendMessageResponse;
import org.avni_integration_service.rwb.dto.NudgeUserRequestDTO;
import org.avni_integration_service.rwb.repository.AvniRwbUserNudgeRepository;
import org.avni_integration_service.rwb.service.RwbUserNudgeErrorService;
import org.avni_integration_service.rwb.service.RwbUserNudgeService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RWBUsersNudgeWorker {
    private static final Logger logger = Logger.getLogger(RWBUsersNudgeWorker.class);
    private final RwbUserNudgeService rwbUserNudgeService;
    private final RwbUserNudgeErrorService rwbUserNudgeErrorService;

    public RWBUsersNudgeWorker(RwbUserNudgeService rwbUserNudgeService, RwbUserNudgeErrorService rwbUserNudgeErrorService) {
        this.rwbUserNudgeService = rwbUserNudgeService;
        this.rwbUserNudgeErrorService = rwbUserNudgeErrorService;
    }

    public void processUsers() {
        AvniRwbUserNudgeRepository.getQueryToFlowIdMap().forEach((queryName, flowId) -> {
            logger.info(String.format("Processing flow %s for query '%s'", flowId, queryName));
            List<NudgeUserRequestDTO> users = rwbUserNudgeService.getUsersForQuery(queryName);
            users.forEach(dto -> processUser(dto, flowId));
        });
    }

    private void processUser(NudgeUserRequestDTO nudgeUserRequestDTO, String flowId) {
        try {
            SendMessageResponse sendMessageResponse = rwbUserNudgeService.nudgeUser(nudgeUserRequestDTO, flowId);
            rwbUserNudgeErrorService.saveUserNudgeStatus(nudgeUserRequestDTO.getUserId(), sendMessageResponse);
        } catch (Exception exception) {
            rwbUserNudgeErrorService.saveUserNudgeError(nudgeUserRequestDTO.getUserId(), exception);
        }
    }
}
