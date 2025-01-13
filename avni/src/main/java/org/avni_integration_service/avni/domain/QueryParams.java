package org.avni_integration_service.avni.domain;

public class QueryParams {
    public String noOfDays;

    public QueryParams(String noOfDays) {
        this.noOfDays = noOfDays;
    }

    public String getNoOfDays() {
        return noOfDays;
    }
}
