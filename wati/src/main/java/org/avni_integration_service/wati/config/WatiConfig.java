package org.avni_integration_service.wati.config;

import org.avni_integration_service.integration_data.context.ContextIntegrationSystem;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.springframework.util.StringUtils;

import java.util.Map;

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

    /**
     * Returns map of queryName → Wati templateName from config keys prefixed with "template."
     * e.g. template.chlorine_refill_reminder=chlorine_refill_reminder
     */
    public Map<String, String> getQueryToTemplateNameMap() {
        return integrationSystemConfigCollection.getConfigsByPrefix("template.");
    }

    /**
     * Returns the Wati template name for a given query and user locale.
     * Tries locale-specific key first (template.<queryName>.<locale>),
     * falls back to default (template.<queryName>).
     */
    public String getTemplateName(String queryName, String locale) {
        if (StringUtils.hasLength(locale)) {
            String localeTemplate = integrationSystemConfigCollection.getConfigValue("template." + queryName + "." + locale);
            if (StringUtils.hasLength(localeTemplate)) return localeTemplate;
        }
        return integrationSystemConfigCollection.getConfigValue("template." + queryName);
    }

    public ContextIntegrationSystem getIntegrationSystem() {
        return integrationSystem;
    }
}
