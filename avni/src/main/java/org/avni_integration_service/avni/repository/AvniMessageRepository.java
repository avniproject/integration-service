package org.avni_integration_service.avni.repository;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.avni.domain.ManualMessageContract;
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

    public String sendMessage(ManualMessageContract manualMessageContract) {
        ResponseEntity<String> responseEntity = avniHttpClient.post("/sendMsg", manualMessageContract, String.class);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            return responseEntity.getBody();
        }
        logger.error(String.format("Failed to sendMessage to user %s, response status code is %s",
                manualMessageContract.getReceiverId(), responseEntity.getStatusCode()));
        throw new HttpServerErrorException(responseEntity.getStatusCode(), responseEntity.getStatusCode().getReasonPhrase(),
                responseEntity.getBody().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
