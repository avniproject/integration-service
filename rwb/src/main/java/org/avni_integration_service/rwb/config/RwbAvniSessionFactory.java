package org.avni_integration_service.rwb.config;

import org.avni_integration_service.avni.client.AvniSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RwbAvniSessionFactory {
    @Value("${rwb.avni.api.url}")
    private String apiUrl;

    @Value("${rwb.avni.impl.username}")
    private String implUser;

    @Value("${rwb.avni.impl.password}")
    private String implPassword;

    @Value("${rwb.avni.authentication.enabled}")
    private boolean authEnabled;

    public AvniSession createSession() {
        return new AvniSession(apiUrl, implUser, implPassword, authEnabled);
    }
}
