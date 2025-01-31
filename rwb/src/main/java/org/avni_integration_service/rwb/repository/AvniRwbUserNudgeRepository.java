package org.avni_integration_service.rwb.repository;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.*;
import org.avni_integration_service.avni.repository.AvniMessageRepository;
import org.avni_integration_service.avni.repository.AvniQueryRepository;
import org.avni_integration_service.rwb.dto.NudgeUserRequestDTO;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AvniRwbUserNudgeRepository {

    private static final Logger logger = Logger.getLogger(AvniMessageRepository.class);
    private final AvniMessageRepository avniMessageRepository;
    private final AvniQueryRepository avniQueryRepository;
    private final String nudgeMessageTemplateId;

    public AvniRwbUserNudgeRepository(AvniMessageRepository avniMessageRepository, AvniQueryRepository avniQueryRepository,
                                      @Value("${rwb.avni.message.template.id}") String nudgeMessageTemplateId) {
        this.avniMessageRepository = avniMessageRepository;
        this.avniQueryRepository = avniQueryRepository;
        this.nudgeMessageTemplateId = nudgeMessageTemplateId;
    }

    public SendMessageResponse sendMessage(NudgeUserRequestDTO nudgeUserRequestDTO) {
        return avniMessageRepository.sendMessage(createMessageRequestToNudgeUser(nudgeUserRequestDTO));
    }

    private ManualMessageContract createMessageRequestToNudgeUser(NudgeUserRequestDTO nudgeUserRequestDTO) {
        ManualMessageContract manualMessageContract = new ManualMessageContract();
        manualMessageContract.setReceiverId(nudgeUserRequestDTO.getUserId());
        manualMessageContract.setReceiverType(ReceiverType.User);
        manualMessageContract.setMessageTemplateId(nudgeMessageTemplateId);
        manualMessageContract.setParameters(new String[]{
                nudgeUserRequestDTO.getUserName(), nudgeUserRequestDTO.getSinceNoOfDays(), nudgeUserRequestDTO.getWithinNoOfDays()});
        manualMessageContract.setScheduledDateTime(new DateTime()); //set current date time
        return manualMessageContract;
    }

    public CustomQueryResponse executeCustomQuery(CustomQueryRequest customQueryRequest) {
        return avniQueryRepository.invokeCustomQuery(customQueryRequest);
    }

}
