package org.avni_integration_service.wati.repository;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.*;
import org.avni_integration_service.avni.repository.AvniMessageRepository;
import org.avni_integration_service.avni.repository.AvniQueryRepository;
import org.avni_integration_service.wati.dto.WatiUserRequestDTO;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

@Component
public class AvniWatiUserMessageRepository {
    private static final Logger logger = Logger.getLogger(AvniWatiUserMessageRepository.class);

    private final AvniMessageRepository avniMessageRepository;
    private final AvniQueryRepository avniQueryRepository;

    public AvniWatiUserMessageRepository(AvniMessageRepository avniMessageRepository, AvniQueryRepository avniQueryRepository) {
        this.avniMessageRepository = avniMessageRepository;
        this.avniQueryRepository = avniQueryRepository;
    }

    public SendMessageResponse sendMessage(WatiUserRequestDTO dto, String templateName) {
        logger.info(String.format("Sending Wati template '%s' to user %s", templateName, dto.getUserId()));
        return avniMessageRepository.sendMessage(buildManualMessageContract(dto, templateName));
    }

    private ManualMessageContract buildManualMessageContract(WatiUserRequestDTO dto, String templateName) {
        ManualMessageContract contract = new ManualMessageContract();
        contract.setReceiverId(dto.getUserId());
        contract.setReceiverType(ReceiverType.User);
        contract.setMessageTemplateId(templateName);
        contract.setParameters(dto.getParameters());
        contract.setScheduledDateTime(new DateTime());
        return contract;
    }

    public CustomQueryResponse executeCustomQuery(CustomQueryRequest customQueryRequest) {
        return avniQueryRepository.invokeCustomQuery(customQueryRequest);
    }
}
