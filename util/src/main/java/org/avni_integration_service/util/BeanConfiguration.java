package org.avni_integration_service.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class BeanConfiguration {
    private final RestTemplate utilRestTemplate;

    @Autowired
    public BeanConfiguration(Environment environment, RestTemplateBuilder restTemplateBuilder) {
        utilRestTemplate = restTemplateBuilder.build();
    }

    @Bean("UtilRestTemplate")
    public RestTemplate utilRestTemplate() {
        return utilRestTemplate;
    }
}
