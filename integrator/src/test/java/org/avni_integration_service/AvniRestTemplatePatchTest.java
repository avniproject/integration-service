package org.avni_integration_service;

import org.avni_integration_service.avni.config.AvniBeanConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AvniRestTemplatePatchTest {
    @Test
    public void theSharedAvniTemplateCanBuildAPatchRequest() throws Exception {
        RestTemplate avniRestTemplate = new AvniBeanConfiguration(null, new RestTemplateBuilder()).avniRestTemplate();

        ClientHttpRequest request = avniRestTemplate.getRequestFactory().createRequest(URI.create("http://localhost:1/api/encounter/x"), HttpMethod.PATCH);

        assertEquals(HttpMethod.PATCH, request.getMethod());
    }
}
