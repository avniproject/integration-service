package org.avni_integration_service.rwb.config;

import org.avni_integration_service.integration_data.context.ContextIntegrationSystem;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.springframework.util.StringUtils;

public class RwbConfig {
    private final IntegrationSystemConfigCollection integrationSystemConfigCollection;
    private final ContextIntegrationSystem integrationSystem;

    public RwbConfig(IntegrationSystemConfigCollection integrationSystemConfigCollection, IntegrationSystem integrationSystem) {
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

    public String getCustomQueryName() {
        return getStringConfigValue("custom_query", "Inactive users");
    }

    public String getSinceNoOfDays() {
        return getStringConfigValue("since_no_of_days", "01");
    }

    public String getWithinNoOfDays() {
        return getStringConfigValue("within_no_of_days", "03");
    }

    public String getMsgTemplateId() {
        return getStringConfigValue("mgs_template_id", "542201");
    }


    public ContextIntegrationSystem getIntegrationSystem() {
        return integrationSystem;
    }
}
