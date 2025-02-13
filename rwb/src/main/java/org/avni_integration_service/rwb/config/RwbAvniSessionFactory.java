package org.avni_integration_service.rwb.config;

import org.avni_integration_service.avni.client.AvniSession;
import org.springframework.stereotype.Component;

@Component
public class RwbAvniSessionFactory {

    private final RwbContextProvider rwbContextProvider;

    public RwbAvniSessionFactory(RwbContextProvider rwbContextProvider) {
        this.rwbContextProvider = rwbContextProvider;
    }

    public AvniSession createSession() {
        RwbConfig rwbConfig = rwbContextProvider.get();
        return new AvniSession(rwbConfig.getApiUrl(), rwbConfig.getAvniImplUser(), rwbConfig.getImplPassword(), rwbConfig.getAuthEnabled());
    }
}
