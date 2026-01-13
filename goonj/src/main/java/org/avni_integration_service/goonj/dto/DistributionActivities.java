package org.avni_integration_service.goonj.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DistributionActivities {

    @JsonProperty("ActivitySourceId")
    private String ActivitySourceId;

    /**
     * No args constructor for use in serialization
     */
    public DistributionActivities() {
    }

    /**
     * @param ActivitySourceId
     */
    public DistributionActivities(String ActivitySourceId) {
        super();
        this.ActivitySourceId = ActivitySourceId;
    }

    @JsonProperty("ActivitySourceId")
    public String getaActivityId() {
        return ActivitySourceId;
    }

    @JsonProperty("ActivitySourceId")
    public void setActivityId(String activitySourceId) {
        this.ActivitySourceId = activitySourceId;
    }

}
