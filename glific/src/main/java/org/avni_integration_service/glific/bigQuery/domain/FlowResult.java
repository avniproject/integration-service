package org.avni_integration_service.glific.bigQuery.domain;

import java.util.HashMap;
import java.util.Map;

import static org.avni_integration_service.util.ObjectJsonMapper.readValue;

public class FlowResult {
    private final Map<String, Object> fields;
    private final Map<String, Object> results;

    public static String STUDENT_CONTACT_NUMBER = "contact_phone";

    public FlowResult(Map<String, Object> fields) {
        this.fields = fields;
        results = readValue((String)this.fields.get("results"), Map.class);
    }

    public String getContactPhone() {
        return (String) fields.get(STUDENT_CONTACT_NUMBER);
    }

    public String getInsertedAt() {
        return (String) fields.get("inserted_at");
    }

    public String getUpdatedAt() {
        return (String) fields.get("updated_at");
    }

    public String getInput(String key) {
        return (String) getResult(key).get("input");
    }

    public String getCategory(String key) {
        return (String) getResult(key).get("category");
    }

    public String getFlowResultId() {
        return (String) fields.get("flowresult_id");
    }

    public String lastUpdatedAt() {
        return (String) fields.get("updated_at");
    }

    public String insertedAt() {
        return (String) fields.get("inserted_at");
    }

    private Map<String, Object> getResult(String key) {
        return (Map<String, Object>) results.getOrDefault(key, new HashMap<>());
    }

    public boolean isComplete() {
        String registrationFlowComplete = this.getInput("registration_flow_complete");
        return registrationFlowComplete != null && registrationFlowComplete.equalsIgnoreCase("Yes");
    }
}
