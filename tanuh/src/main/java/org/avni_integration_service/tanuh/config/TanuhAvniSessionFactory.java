package org.avni_integration_service.tanuh.config;

import org.avni_integration_service.avni.client.AvniSession;
import org.springframework.stereotype.Component;

@Component
public class TanuhAvniSessionFactory {

    private final TanuhContextProvider tanuhContextProvider;

    public TanuhAvniSessionFactory(TanuhContextProvider tanuhContextProvider) {
        this.tanuhContextProvider = tanuhContextProvider;
    }

    public AvniSession createSession() {
        TanuhConfig tanuhConfig = tanuhContextProvider.get();
        return new AvniSession(tanuhConfig.getApiUrl(), tanuhConfig.getAvniImplUser(), tanuhConfig.getImplPassword(), tanuhConfig.getAuthEnabled());
    }
}
