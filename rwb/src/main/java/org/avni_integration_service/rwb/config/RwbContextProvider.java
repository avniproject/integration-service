package org.avni_integration_service.rwb.config;

import org.springframework.stereotype.Component;

@Component
public class RwbContextProvider {
    private static final ThreadLocal<RwbConfig> rwbConfigs = new ThreadLocal<>();

    public void set(RwbConfig rwbConfig) {
        rwbConfigs.set(rwbConfig);
    }

    public RwbConfig get() {
        RwbConfig rwbConfig = rwbConfigs.get();
        if (rwbConfig == null)
            throw new IllegalStateException("No Rwb config available. Have you called package org.avni_integration_service.rwb.config.RwbContextProvider.set.");
        return rwbConfig;
    }
}