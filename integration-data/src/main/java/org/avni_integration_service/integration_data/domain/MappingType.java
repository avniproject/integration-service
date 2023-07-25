package org.avni_integration_service.integration_data.domain;

import org.avni_integration_service.integration_data.domain.framework.NamedIntegrationSpecificEntity;

import javax.persistence.Entity;
import java.util.UUID;

@Entity
public class MappingType extends NamedIntegrationSpecificEntity {
    public MappingType() {
    }
    public MappingType(String name, IntegrationSystem integrationSystem) {
        this.setName(name);
        this.setIntegrationSystem(integrationSystem);
        this.setUuid(UUID.randomUUID().toString());
    }
}
