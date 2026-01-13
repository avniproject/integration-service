package org.avni_integration_service.web.contract;

import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfig;

public class IntegrationSystemConfigContract extends BaseEntityContract {
    private String key;
    private String value;
    private boolean isSecret;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isSecret() {
        return isSecret;
    }

    public void setSecret(boolean secret) {
        isSecret = secret;
    }

    public static IntegrationSystemConfigContract fromIntegrationSystemConfig(IntegrationSystemConfig integrationSystemConfig) {
        IntegrationSystemConfigContract integrationSystemConfigContract = new IntegrationSystemConfigContract();
        integrationSystemConfigContract.setUuid(integrationSystemConfig.getUuid());
        integrationSystemConfigContract.setVoided(integrationSystemConfig.isVoided());
        integrationSystemConfigContract.setSecret(integrationSystemConfig.isSecret());
        integrationSystemConfigContract.setKey(integrationSystemConfig.getKey());
        integrationSystemConfigContract.setValue(integrationSystemConfig.isSecret() ? "" : integrationSystemConfig.getValue());
        return integrationSystemConfigContract;
    }
}
