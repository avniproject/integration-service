package org.avni_integration_service.avni.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class CustomQueryResponse {

    @JsonProperty("headers")
    List<Object> headers;

    @JsonProperty("data")
    List<List<Object>> data;

    @JsonProperty("total")
    long total;

    public List<Object> getHeaders() {
        return headers;
    }

    public List<List<Object>> getData() {
        return data;
    }

    public long getTotal() {
        return total;
    }
}
