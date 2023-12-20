package org.avni_integration_service.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import org.apache.log4j.Logger;

@Component
public class HealthCheckService {
    private static final String PING_BASE_URL = "https://hc-ping.com";

    private static final Logger logger = Logger.getLogger(HealthCheckService.class);

    private final RestTemplate restTemplate;

    @Value("${healthcheck.ping.key}")
    private String healthCheckPingKey;

    @Autowired
    public HealthCheckService(@Qualifier("UtilRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void ping(String slug, Status status) {
        logger.info("Health check ping for slug:"+ slug+" status: "+ status);
        try {
            if (!healthCheckPingKey.equals("dummy"))
                restTemplate.exchange(URI.create(String.format("%s/%s/%s/%s", PING_BASE_URL, healthCheckPingKey, slug, status.getValue())), HttpMethod.GET, null, String.class);
        } catch(Exception e) {
            logger.error("Health check ping failed:", e);
        }
    }

    public void start(String slug) {
        ping(slug, Status.START);
    }
    public void success(String slug) {
        ping(slug, Status.SUCCESS);
    }
    public void failure(String slug) {
        ping(slug,  Status.FAILURE);
    }

    public enum Status {
        START("start"), SUCCESS("0"), FAILURE("fail");
        String value;

        Status(String status) {
            this.value = status;
        }

        public String getValue() {
            return value;
        }
    }
}
