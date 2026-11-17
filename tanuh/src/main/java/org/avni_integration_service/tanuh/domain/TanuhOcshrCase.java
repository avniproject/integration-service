package org.avni_integration_service.tanuh.domain;

import org.avni_integration_service.integration_data.domain.framework.BaseIntegrationSpecificEntity;

import javax.persistence.*;
import java.time.LocalDateTime;

// One screening the job sent to Tanuh's high-risk model service, or marked not sent (V2_4_10).
@Entity
@Table(name = "tanuh_ocshr_case")
@AssociationOverride(name = "integrationSystem",
        joinColumns = @JoinColumn(name = "integration_system_id", nullable = false))
@AttributeOverride(name = "isVoided",
        column = @Column(name = "is_voided", nullable = false))
public class TanuhOcshrCase extends BaseIntegrationSpecificEntity {
    @Column(nullable = false)
    private String screeningUuid;

    @Column
    private String imageUris;

    @Column
    private String callMade;

    @Column(nullable = false)
    private LocalDateTime createdDateTime = LocalDateTime.now();

    public String getScreeningUuid() {
        return screeningUuid;
    }

    public void setScreeningUuid(String screeningUuid) {
        this.screeningUuid = screeningUuid;
    }

    public String getImageUris() {
        return imageUris;
    }

    public void setImageUris(String imageUris) {
        this.imageUris = imageUris;
    }

    public String getCallMade() {
        return callMade;
    }

    public void setCallMade(String callMade) {
        this.callMade = callMade;
    }

    public LocalDateTime getCreatedDateTime() {
        return createdDateTime;
    }
}
