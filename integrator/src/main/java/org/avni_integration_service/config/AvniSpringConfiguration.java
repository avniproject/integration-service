package org.avni_integration_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.avni_integration_service.util.ObjectJsonMapper;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class AvniSpringConfiguration {

    @Bean
    WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> enableDefaultServlet() {
        return (factory) -> factory.setRegisterDefaultServlet(true);
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return ObjectJsonMapper.objectMapper;
    }
}
