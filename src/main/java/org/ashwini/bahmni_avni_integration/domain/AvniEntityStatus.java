package org.ashwini.bahmni_avni_integration.domain;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "avni_entity_status")
public class AvniEntityStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "read_upto", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private java.util.Date readUpto;

    @Column(name = "entity_type")
    @Enumerated(EnumType.STRING)
    private AvniEntityType entityType;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getReadUpto() {
        return readUpto;
    }

    public void setReadUpto(Date readUpto) {
        this.readUpto = readUpto;
    }

    public AvniEntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(AvniEntityType entityType) {
        this.entityType = entityType;
    }
}