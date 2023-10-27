package org.avni_integration_service.lahi.domain;

import java.util.List;
import java.util.Map;

import static org.avni_integration_service.lahi.domain.StudentConstants.FLOWRESULT_ID;

public abstract class LahiEntity {
    protected Map<String, Object> response;

    protected LahiEntity(Map<String, Object> response) {
        this.response = response;
    }

    public abstract List<String> getObservationFields();
    public abstract Object getValue(String responseField);

    public String getFlowResult() {
        return response.get(FLOWRESULT_ID).toString();
    }
}
