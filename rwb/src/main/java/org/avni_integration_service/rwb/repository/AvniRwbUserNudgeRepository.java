package org.avni_integration_service.rwb.repository;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.*;
import org.avni_integration_service.avni.repository.AvniMessageRepository;
import org.avni_integration_service.avni.repository.AvniQueryRepository;
import org.avni_integration_service.rwb.config.RwbContextProvider;
import org.avni_integration_service.rwb.dto.NudgeUserRequestDTO;
import org.springframework.stereotype.Component;

@Component
public class AvniRwbUserNudgeRepository {

    private static final Logger logger = Logger.getLogger(AvniMessageRepository.class);
    private final AvniMessageRepository avniMessageRepository;
    private final AvniQueryRepository avniQueryRepository;
    private final RwbContextProvider rwbContextProvider;

    public AvniRwbUserNudgeRepository(AvniMessageRepository avniMessageRepository, AvniQueryRepository avniQueryRepository, RwbContextProvider rwbContextProvider) {
        this.avniMessageRepository = avniMessageRepository;
        this.avniQueryRepository = avniQueryRepository;
        this.rwbContextProvider = rwbContextProvider;
    }

    public SendMessageResponse startFlowForContact(NudgeUserRequestDTO nudgeUserRequestDTO) {
        return avniMessageRepository.startFlowForContact(createMessageRequestToNudgeUser(nudgeUserRequestDTO));
    }

    private StartFlowForContactRequest createMessageRequestToNudgeUser(NudgeUserRequestDTO nudgeUserRequestDTO) {
        StartFlowForContactRequest startFlowForContactRequest = new StartFlowForContactRequest();
        startFlowForContactRequest.setReceiverId(nudgeUserRequestDTO.getUserId());
        startFlowForContactRequest.setReceiverType(ReceiverType.User);
        startFlowForContactRequest.setFlowId(rwbContextProvider.get().getFlowId());
        startFlowForContactRequest.setParameters(new String[]{
                nudgeUserRequestDTO.getUserName(), nudgeUserRequestDTO.getSinceNoOfDays(), nudgeUserRequestDTO.getWithinNoOfDays()});
        return startFlowForContactRequest;
    }

    public CustomQueryResponse executeCustomQuery(CustomQueryRequest customQueryRequest) {
        return avniQueryRepository.invokeCustomQuery(customQueryRequest);
    }

}
