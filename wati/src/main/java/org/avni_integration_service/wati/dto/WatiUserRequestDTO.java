package org.avni_integration_service.wati.dto;

public class WatiUserRequestDTO {

    private final String userId;
    private final String locale;
    private final String[] parameters;

    public WatiUserRequestDTO(String userId, String locale, String[] parameters) {
        this.userId = userId;
        this.locale = locale;
        this.parameters = parameters;
    }

    public String getUserId() {
        return userId;
    }

    public String getLocale() {
        return locale;
    }

    public String[] getParameters() {
        return parameters;
    }
}
