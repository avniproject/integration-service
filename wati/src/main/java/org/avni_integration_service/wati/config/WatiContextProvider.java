package org.avni_integration_service.wati.config;

import org.springframework.stereotype.Component;

@Component
public class WatiContextProvider {
    private static final ThreadLocal<WatiConfig> watiConfigs = new ThreadLocal<>();

    public void set(WatiConfig watiConfig) {
        watiConfigs.set(watiConfig);
    }

    public WatiConfig get() {
        WatiConfig watiConfig = watiConfigs.get();
        if (watiConfig == null)
            throw new IllegalStateException("No Wati config available. Have you called WatiContextProvider.set?");
        return watiConfig;
    }

    public static void clear() {
        watiConfigs.remove();
    }
}
