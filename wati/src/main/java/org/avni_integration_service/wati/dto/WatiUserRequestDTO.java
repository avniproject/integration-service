package org.avni_integration_service.wati.dto;

public class WatiUserRequestDTO {

    private final String phoneNumber;
    private final String locale;
    private final String[] parameters;

    public WatiUserRequestDTO(String phoneNumber, String locale, String[] parameters) {
        this.phoneNumber = phoneNumber;
        this.locale = locale;
        this.parameters = parameters;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getLocale() {
        return locale;
    }

    public String[] getParameters() {
        return parameters;
    }
}
