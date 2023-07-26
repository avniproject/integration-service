package org.avni_integration_service.integration_data.domain;

import org.avni_integration_service.integration_data.domain.framework.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import java.util.UUID;

@Entity
public class IgnoredIntegratingConcept extends BaseEntity {
    @Column
    private String conceptId;

    public IgnoredIntegratingConcept() {
    }

    public IgnoredIntegratingConcept(String conceptId) {
        this.conceptId = conceptId;
        this.setUuid(UUID.randomUUID().toString());
    }

    public String getConceptId() {
        return conceptId;
    }
}
