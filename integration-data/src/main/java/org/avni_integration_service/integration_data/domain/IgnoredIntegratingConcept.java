package org.avni_integration_service.integration_data.domain;

import org.avni_integration_service.integration_data.domain.framework.BaseEntity;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class IgnoredIntegratingConcept extends BaseEntity {
    @Column
    private String conceptId;

    public IgnoredIntegratingConcept() {
        super();
    }

    public IgnoredIntegratingConcept(String conceptId) {
        super();
        this.conceptId = conceptId;
    }

    public String getConceptId() {
        return conceptId;
    }
}
