package org.avni_integration_service.avni.domain;

import org.joda.time.DateTime;

public class CustomQueryRequest {
    public String name;
    public QueryParams queryParams;

    public CustomQueryRequest(String name, int numberOfDays) {
        this.name = name;
        this.queryParams = new QueryParams(DateTime.now().minusDays(numberOfDays).toDate());
    }

    public String getName() {
        return name;
    }

    public QueryParams getQueryParams() {
        return queryParams;
    }
}

