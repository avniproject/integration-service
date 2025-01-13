package org.avni_integration_service.avni.domain;

public class CustomQueryRequest {

    public String name;
    public QueryParams queryParams;

    public CustomQueryRequest(String name, String noOfDays) {
        this.name = name;
        this.queryParams = new QueryParams(noOfDays);
    }

    public String getName() {
        return name;
    }

    public QueryParams getQueryParams() {
        return queryParams;
    }
}

