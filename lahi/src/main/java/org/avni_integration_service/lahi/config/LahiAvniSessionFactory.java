package org.avni_integration_service.lahi.config;

import org.avni_integration_service.avni.client.AvniSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LahiAvniSessionFactory {
    @Value("${lahi.avni.api.url}")
    private String apiUrl;

    @Value("${lahi.avni.impl.username}")
    private String implUser;

    @Value("${lahi.avni.impl.password}")
    private String implPassword;

    @Value("${lahi.avni.authentication.enabled}")
    private boolean authEnabled;

    public AvniSession createSession() {
        return new AvniSession(apiUrl, implUser, implPassword, authEnabled);
    }
}
