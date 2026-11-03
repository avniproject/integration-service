package org.avni_integration_service.tanuh.config;

import org.avni_integration_service.tanuh.domain.TanuhConcepts;
import org.springframework.stereotype.Component;

@Component
public class TanuhContextProvider {
    private static final ThreadLocal<TanuhConfig> tanuhConfigs = new ThreadLocal<>();
    private static final ThreadLocal<TanuhConcepts> tanuhConcepts = new ThreadLocal<>();

    public void set(TanuhConfig tanuhConfig) {
        tanuhConfigs.set(tanuhConfig);
    }

    public TanuhConfig get() {
        TanuhConfig tanuhConfig = tanuhConfigs.get();
        if (tanuhConfig == null)
            throw new IllegalStateException("No Tanuh config available. Have you called TanuhContextProvider.set?");
        return tanuhConfig;
    }

    // The run's names, loaded by the job from the organisation's mapping rows.
    public void setConcepts(TanuhConcepts concepts) {
        tanuhConcepts.set(concepts);
    }

    public TanuhConcepts getConcepts() {
        TanuhConcepts concepts = tanuhConcepts.get();
        if (concepts == null)
            throw new IllegalStateException("No Tanuh names available. Have you called TanuhContextProvider.setConcepts?");
        return concepts;
    }

    public static void clear() {
        tanuhConfigs.remove();
        tanuhConcepts.remove();
    }
}
