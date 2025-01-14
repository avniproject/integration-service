package org.avni_integration_service.avni.repository;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.avni.domain.ManualMessageContract;
import org.avni_integration_service.avni.domain.SendMessageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;

import java.nio.charset.StandardCharsets;

@Component
public class AvniMessageRepository {

    private static final Logger logger = Logger.getLogger(AvniMessageRepository.class);

    private final AvniHttpClient avniHttpClient;

    public AvniMessageRepository(AvniHttpClient avniHttpClient) {
        this.avniHttpClient = avniHttpClient;
    }

    public SendMessageResponse sendMessage(ManualMessageContract manualMessageContract) {
        ResponseEntity<SendMessageResponse> responseEntity = avniHttpClient.post("/web/message/sendMsg", manualMessageContract, SendMessageResponse.class);
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            logger.error(String.format("SendMessage to user %s, deliveryStatus %s, response status code is %s", manualMessageContract.getReceiverId(),
                    responseEntity.getBody().getMessageDeliveryStatus(), responseEntity.getStatusCode()));
        }
        return responseEntity.getBody();
    }
}