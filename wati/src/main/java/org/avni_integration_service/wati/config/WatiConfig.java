package org.avni_integration_service.wati.config;

import org.avni_integration_service.integration_data.context.ContextIntegrationSystem;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.stream.Collectors;

public class WatiConfig {
    private final IntegrationSystemConfigCollection integrationSystemConfigCollection;
    private final ContextIntegrationSystem integrationSystem;

    public WatiConfig(IntegrationSystemConfigCollection integrationSystemConfigCollection, IntegrationSystem integrationSystem) {
        this.integrationSystemConfigCollection = integrationSystemConfigCollection;
        this.integrationSystem = new ContextIntegrationSystem(integrationSystem);
    }

    private String getStringConfigValue(String key, String defaultValue) {
        String configValue = integrationSystemConfigCollection.getConfigValue(key);
        return StringUtils.hasLength(configValue) ? configValue : defaultValue;
    }

    public String getApiUrl() {
        return getStringConfigValue("avni_api_url", "https://app.avniproject.org");
    }

    public String getAvniImplUser() {
        return getStringConfigValue("avni_user", "dummy");
    }

    public String getImplPassword() {
        return getStringConfigValue("avni_password", "dummy");
    }

    public boolean getAuthEnabled() {
        return Boolean.parseBoolean(getStringConfigValue("avni_auth_enabled", "true"));
    }

    public String getEnvironment() {
        return getStringConfigValue("int_env", null);
    }

    public Map<String, String> getFlowToQueryMap() {
        return integrationSystemConfigCollection.getConfigsByPrefix("flow.").entrySet().stream()
                .filter(e -> e.getKey().endsWith(".custom_query"))
                .collect(Collectors.toMap(
                        e -> e.getKey().substring(0, e.getKey().length() - ".custom_query".length()),
                        Map.Entry::getValue));
    }

    public WatiFlowConfig getFlowConfig(String flowName) {
        String prefix = "flow." + flowName + ".";
        boolean enabled = Boolean.parseBoolean(getStringConfigValue(prefix + "enabled", "true"));
        String templateParamsStr = getStringConfigValue(prefix + "template_params", "");
        String[] templateParams = StringUtils.hasLength(templateParamsStr) ? templateParamsStr.split(",") : new String[0];
        String entityType = getStringConfigValue(prefix + "entity_type", "encounter");
        return new WatiFlowConfig(
                flowName,
                integrationSystemConfigCollection.getConfigValue(prefix + "custom_query"),
                enabled,
                templateParams,
                entityType,
                Integer.parseInt(getStringConfigValue(prefix + "cooldown_days", "7")),
                Integer.parseInt(getStringConfigValue(prefix + "max_retries", "3")),
                Integer.parseInt(getStringConfigValue(prefix + "retry_interval_hours", "24")));
    }

    public String getTemplateName(String flowName, String locale) {
        if (StringUtils.hasLength(locale)) {
            String localeTemplate = integrationSystemConfigCollection.getConfigValue("flow." + flowName + ".template_name." + locale);
            if (StringUtils.hasLength(localeTemplate)) return localeTemplate;
        }
        return integrationSystemConfigCollection.getConfigValue("flow." + flowName + ".template_name");
    }

    public String getWatiApiUrl() {
        return getStringConfigValue("wati_api_url", null);
    }

    public String getWatiApiKey() {
        return getStringConfigValue("wati_api_key", null);
    }

    public ContextIntegrationSystem getIntegrationSystem() {
        return integrationSystem;
    }
}
