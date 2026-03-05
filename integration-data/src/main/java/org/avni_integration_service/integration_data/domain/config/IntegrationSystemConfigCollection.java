package org.avni_integration_service.integration_data.domain.config;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IntegrationSystemConfigCollection {
    private final List<IntegrationSystemConfig> integrationSystemConfigs;

    private static final String MAIN_SCHEDULED_JOB_CRON = "main.scheduled.job.cron";
    private static final String ERROR_SCHEDULED_JOB_CRON = "error.scheduled.job.cron";

    public IntegrationSystemConfigCollection(List<IntegrationSystemConfig> integrationSystemConfigs) {
        this.integrationSystemConfigs = integrationSystemConfigs;
    }

    private String getConfigValue(String key, String defaultValue) {
        String value = getConfigValue(key);
        return value == null ? defaultValue : value;
    }

    public String getConfigValue(String key) {
        IntegrationSystemConfig integrationSystemConfig = integrationSystemConfigs.stream().filter(x -> x.getKey().equals(key)).findFirst().orElse(null);
        return integrationSystemConfig == null ? null : integrationSystemConfig.getValue();
    }

    public String getMainScheduledJobCron() {
        return this.getConfigValue(MAIN_SCHEDULED_JOB_CRON, "-");
    }

    public String getErrorScheduledJobCron() {
        return this.getConfigValue(ERROR_SCHEDULED_JOB_CRON, "-");
    }

    public Map<String, String> getConfigsByPrefix(String prefix) {
        return integrationSystemConfigs.stream()
                .filter(x -> x.getKey().startsWith(prefix))
                .collect(Collectors.toMap(
                        x -> x.getKey().substring(prefix.length()),
                        IntegrationSystemConfig::getValue));
    }
}
