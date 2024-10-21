package org.avni_integration_service.integration_data.domain.framework;

import org.joda.time.DateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;
import java.util.Date;

@MappedSuperclass
@EntityListeners({AuditingEntityListener.class})
public abstract class AuditableEntity extends BaseIntegrationSpecificEntity{
    @Column(name = "created_date_time")
    @CreatedDate
    @NotNull(message = "createdDateTime can't be null")
    private Date createdDateTime;

    @Column(name = "last_modified_date_time")
    @LastModifiedDate
    @NotNull(message = "lastModifiedDateTime can't be null")
    private Date lastModifiedDateTime;


    public void setCreatedDateTime(DateTime createdDateTime) {
        this.createdDateTime = createdDateTime.toDate();
    }

    public DateTime getCreatedDateTime() {
        return new DateTime(createdDateTime);
    }

    public void setLastModifiedDateTime(DateTime lastModifiedDateTime) {
        this.lastModifiedDateTime = lastModifiedDateTime.toDate();
    }

    public DateTime getLastModifiedDateTime() {
        return new DateTime(lastModifiedDateTime);
    }

}
