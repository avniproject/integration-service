package org.avni_integration_service.avni.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryParams {
    public Date cutOffDate;

    @JsonProperty("flow_id")
    public String flowId;

    public QueryParams(Date cutOffDate) {
        this.cutOffDate = cutOffDate;
    }

    public QueryParams(String flowId) {
        this.flowId = flowId;
    }

    public Date getCutOffDate() {
        return cutOffDate;
    }
}
