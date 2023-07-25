package org.avni_integration_service.integration_data.domain;

import org.avni_integration_service.integration_data.domain.framework.BaseEntity;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "constants") //constant is reserved in postgres
public class Constant extends BaseEntity {
    @Column
    private String key;

    @Column
    private String value;

    public Constant() {
    }

    public Constant(String key, String value) {
        this.key = key;
        this.value = value;
        this.setUuid(UUID.randomUUID().toString());
    }

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
}
