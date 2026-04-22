package org.avni_integration_service.wati.config;

public class WatiFlowConfig {
    private final String flowName;
    private final String customQueryName;
    private final int cooldownDays;
    private final int maxRetries;
    private final int retryIntervalHours;

    public WatiFlowConfig(String flowName, String customQueryName, int cooldownDays, int maxRetries, int retryIntervalHours) {
        this.flowName = flowName;
        this.customQueryName = customQueryName;
        this.cooldownDays = cooldownDays;
        this.maxRetries = maxRetries;
        this.retryIntervalHours = retryIntervalHours;
    }

    public String getFlowName() { return flowName; }
    public String getCustomQueryName() { return customQueryName; }
    public int getCooldownDays() { return cooldownDays; }
    public int getMaxRetries() { return maxRetries; }
    public int getRetryIntervalHours() { return retryIntervalHours; }
}
