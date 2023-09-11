package org.avni_integration_service.lahi.repository;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GlificRepository {

    private static final Logger logger = Logger.getLogger(GlificRepository.class);

    private final RestTemplate lahiRestTemplate;

    public GlificRepository(@Qualifier("LahiRestTemplate") RestTemplate restTemplate) {
        // todo, remove if not needed this whole repository
        this.lahiRestTemplate = restTemplate;

    }


}
