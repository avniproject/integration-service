package org.avni_integration_service.wati.config;

public class WatiFlowConfig {
    private final String flowName;
    private final String customQueryName;
    private final boolean enabled;
    private final String[] templateParams;
    private final String entityType;
    private final int cooldownDays;
    private final int maxRetries;
    private final int retryIntervalHours;

    public WatiFlowConfig(String flowName, String customQueryName, boolean enabled, String[] templateParams,
                          String entityType, int cooldownDays, int maxRetries, int retryIntervalHours) {
        this.flowName = flowName;
        this.customQueryName = customQueryName;
        this.enabled = enabled;
        this.templateParams = templateParams;
        this.entityType = entityType;
        this.cooldownDays = cooldownDays;
        this.maxRetries = maxRetries;
        this.retryIntervalHours = retryIntervalHours;
    }

    public String getFlowName() { return flowName; }
    public String getCustomQueryName() { return customQueryName; }
    public boolean isEnabled() { return enabled; }
    public String[] getTemplateParams() { return templateParams; }
    public String getEntityType() { return entityType; }
    public int getCooldownDays() { return cooldownDays; }
    public int getMaxRetries() { return maxRetries; }
    public int getRetryIntervalHours() { return retryIntervalHours; }
}
