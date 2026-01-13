package org.avni_integration_service.rwb.dto;

public class NudgeUserRequestDTO {

    String userId;
    String userName;
    String sinceNoOfDays;
    String withinNoOfDays;

    public NudgeUserRequestDTO(String userId, String userName, String sinceNoOfDays, String withinNoOfDays) {
        this.userId = userId;
        this.userName = userName;
        this.sinceNoOfDays = sinceNoOfDays;
        this.withinNoOfDays = withinNoOfDays;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getSinceNoOfDays() {
        return sinceNoOfDays;
    }

    public String getWithinNoOfDays() {
        return withinNoOfDays;
    }
}
