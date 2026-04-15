package org.avni_integration_service.wati.config;

import org.avni_integration_service.avni.client.AvniSession;
import org.springframework.stereotype.Component;

@Component
public class WatiAvniSessionFactory {

    private final WatiContextProvider watiContextProvider;

    public WatiAvniSessionFactory(WatiContextProvider watiContextProvider) {
        this.watiContextProvider = watiContextProvider;
    }

    public AvniSession createSession() {
        WatiConfig watiConfig = watiContextProvider.get();
        return new AvniSession(watiConfig.getApiUrl(), watiConfig.getAvniImplUser(), watiConfig.getImplPassword(), watiConfig.getAuthEnabled());
    }
}
