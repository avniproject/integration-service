package org.avni_integration_service.integration_data.domain;

import org.avni_integration_service.integration_data.domain.framework.NamedIntegrationSpecificEntity;

import javax.persistence.Entity;

@Entity
public class MappingGroup extends NamedIntegrationSpecificEntity {
    public MappingGroup() {
        super();
    }

    public MappingGroup(String name, IntegrationSystem integrationSystem) {
        super();
        this.setName(name);
        this.setIntegrationSystem(integrationSystem);
    }
}
