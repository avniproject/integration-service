package org.avni_integration_service.integration_data.domain.framework;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column
    @NotNull
    private String uuid;

    @Column
    private boolean isVoided;

    public BaseEntity() {
        super();
        uuid = UUID.randomUUID().toString();
        isVoided = false;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isVoided() {
        return isVoided;
    }

    public void setVoided(boolean voided) {
        isVoided = voided;
    }
}
