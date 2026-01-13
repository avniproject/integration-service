package org.avni_integration_service.integration_data.domain.config;

import org.avni_integration_service.integration_data.domain.framework.BaseIntegrationSpecificEntity;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class IntegrationSystemConfig extends BaseIntegrationSpecificEntity {
    @Column
    private String key;
    @Column
    private String value;
    @Column
    private boolean isSecret;

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public boolean isSecret() {
        return isSecret;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setSecret(boolean secret) {
        isSecret = secret;
    }

    @Override
    public String toString() {
        return "{" +
                "key='" + key + '\'' +
                '}';
    }
}
