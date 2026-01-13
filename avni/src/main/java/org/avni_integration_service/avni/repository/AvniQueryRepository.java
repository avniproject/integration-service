package org.avni_integration_service.avni.repository;


import org.apache.log4j.Logger;
import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.avni.domain.CustomQueryRequest;
import org.avni_integration_service.avni.domain.CustomQueryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;

@Component
public class AvniQueryRepository {

    private static final Logger logger = Logger.getLogger(AvniQueryRepository.class);

    private final AvniHttpClient avniHttpClient;

    public AvniQueryRepository(AvniHttpClient avniHttpClient) {
        this.avniHttpClient = avniHttpClient;
    }

    public CustomQueryResponse invokeCustomQuery(CustomQueryRequest customQueryRequest) {
        ResponseEntity<CustomQueryResponse> responseEntity = avniHttpClient.post("/executeQuery", customQueryRequest, CustomQueryResponse.class);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            return responseEntity.getBody();
        }
        logger.error(String.format("Failed to invokeCustomQuery, response status code is %s", responseEntity.getStatusCode()));
        throw new HttpServerErrorException(responseEntity.getStatusCode(), responseEntity.getStatusCode().getReasonPhrase());
    }
}