package org.avni_integration_service.rwb.dto;

public class NudgeUserRequestDTO {

    String userId;
    String userName;
    String sinceDateTimeString;

    public NudgeUserRequestDTO(String userId, String userName, String sinceDateTimeString) {
        this.userId = userId;
        this.userName = userName;
        this.sinceDateTimeString = sinceDateTimeString;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getSinceDateTimeString() {
        return sinceDateTimeString;
    }

    public void setSinceDateTimeString(String sinceDateTimeString) {
        this.sinceDateTimeString = sinceDateTimeString;
    }
}
