package org.avni_integration_service.rwb.repository;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.CustomQueryRequest;
import org.avni_integration_service.avni.domain.CustomQueryResponse;
import org.avni_integration_service.avni.domain.ManualMessageContract;
import org.avni_integration_service.avni.domain.ReceiverType;
import org.avni_integration_service.avni.repository.AvniMessageRepository;
import org.avni_integration_service.avni.repository.AvniQueryRepository;
import org.springframework.stereotype.Component;
import org.joda.time.DateTime;

@Component
public class AvniRwbUserNudgeRepository {

    private static final Logger logger = Logger.getLogger(AvniMessageRepository.class);
    private final AvniMessageRepository avniMessageRepository;
    private final AvniQueryRepository avniQueryRepository;

    public AvniRwbUserNudgeRepository(AvniMessageRepository avniMessageRepository, AvniQueryRepository avniQueryRepository) {
        this.avniMessageRepository = avniMessageRepository;
        this.avniQueryRepository = avniQueryRepository;
    }

    public void sendMessage(String userId) {
        avniMessageRepository.sendMessage(createMessageRequestToNudgeUser(userId));
    }

    private ManualMessageContract createMessageRequestToNudgeUser(String userId) {
        ManualMessageContract manualMessageContract = new ManualMessageContract();
        manualMessageContract.setReceiverId(userId); //TODO use query response userId itr value
        manualMessageContract.setReceiverType(ReceiverType.User);
        manualMessageContract.setMessageTemplateId("333223"); //TODO using welcome otp message-template. make it configurable
        manualMessageContract.setParameters(new String[]{"dummy@rwbniti", "dummy"}); //TODO set valid params required as per template
        manualMessageContract.setScheduledDateTime(new DateTime()); //set current date time
        return manualMessageContract;
    }

    public CustomQueryResponse executeCustomQuery(CustomQueryRequest customQueryRequest) {
        return avniQueryRepository.invokeCustomQuery(customQueryRequest);
    }

}
