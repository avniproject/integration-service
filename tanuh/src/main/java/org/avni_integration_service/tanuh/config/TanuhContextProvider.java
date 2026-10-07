package org.avni_integration_service.tanuh.config;

import org.springframework.stereotype.Component;

@Component
public class TanuhContextProvider {
    private static final ThreadLocal<TanuhConfig> tanuhConfigs = new ThreadLocal<>();

    public void set(TanuhConfig tanuhConfig) {
        tanuhConfigs.set(tanuhConfig);
    }

    public TanuhConfig get() {
        TanuhConfig tanuhConfig = tanuhConfigs.get();
        if (tanuhConfig == null)
            throw new IllegalStateException("No Tanuh config available. Have you called TanuhContextProvider.set?");
        return tanuhConfig;
    }

    public static void clear() {
        tanuhConfigs.remove();
    }
}
