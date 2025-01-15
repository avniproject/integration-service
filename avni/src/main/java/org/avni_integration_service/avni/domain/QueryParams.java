package org.avni_integration_service.avni.domain;

import java.util.Date;

public class QueryParams {
    public Date cutOffDate;

    public QueryParams(Date cutOffDate) {
        this.cutOffDate = cutOffDate;
    }

    public Date getCutOffDate() {
        return cutOffDate;
    }
}
