package org.avni_integration_service.integration_data.context;

public class IntegrationContext {
    private static final ThreadLocal<ContextIntegrationSystem> integrationContext = new ThreadLocal<>();

    public static void set(ContextIntegrationSystem contextIntegrationSystem) {
        integrationContext.set(contextIntegrationSystem);
    }

    public static void removeContext() {
        integrationContext.remove();
    }

    public static ContextIntegrationSystem get() {
        ContextIntegrationSystem integrationSystem = integrationContext.get();
        if (integrationSystem == null)
            throw new IllegalStateException("No context available");
        return integrationSystem;
    }

    public static int getIntegrationSystemId() {
        return IntegrationContext.get().getId();
    }
}
