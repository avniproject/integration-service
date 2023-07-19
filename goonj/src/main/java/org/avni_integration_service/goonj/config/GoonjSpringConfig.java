package org.avni_integration_service.goonj.config;

import org.avni_integration_service.goonj.service.TokenService;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;

@Component
public class GoonjSpringConfig {
    private final TokenService tokenService;
    private final IntegrationSystemRepository integrationSystemRepository;

    @Autowired
    public GoonjSpringConfig(TokenService tokenService, IntegrationSystemRepository integrationSystemRepository) {
        this.tokenService = tokenService;
        this.integrationSystemRepository = integrationSystemRepository;
    }

    @Bean("GoonjRestTemplate")
    RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplateBuilder()
                .interceptors((httpRequest, bytes, execution) -> {
                    httpRequest.getHeaders().add(HttpHeaders.AUTHORIZATION,
                            "Bearer " + tokenService.getRefreshedToken().getTokenValue());
                    httpRequest.getHeaders().remove(HttpHeaders.ACCEPT);
                    return execution.execute(httpRequest, bytes);
                })
                .build();
        restTemplate.setMessageConverters(Collections.singletonList(new MappingJackson2HttpMessageConverter()));
        return restTemplate;
    }

    @Bean("GoonjIntegrationSystem")
    public IntegrationSystem getGoonjIntegrationSystem() {
        return integrationSystemRepository.findBySystemType(IntegrationSystem.IntegrationSystemType.Goonj);
    }
}
