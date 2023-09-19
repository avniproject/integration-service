package org.avni_integration_service.lahi.domain;

public class GlificStudent {
    private String name;
    private String id;
    private String status;
    private String completed_at;
    private String contact_id;
    private String contact_phone;
    private String flow_id;
    private String results;
    private String updated_at;
    private String inserted_at;
    private GlificStudentResult glificStudentResult;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCompleted_at() {
        return completed_at;
    }

    public void setCompleted_at(String completed_at) {
        this.completed_at = completed_at;
    }

    public String getContact_id() {
        return contact_id;
    }

    public void setContact_id(String contact_id) {
        this.contact_id = contact_id;
    }

    public String getContact_phone() {
        return contact_phone;
    }

    public void setContact_phone(String contact_phone) {
        this.contact_phone = contact_phone;
    }

    public String getFlow_id() {
        return flow_id;
    }

    public void setFlow_id(String flow_id) {
        this.flow_id = flow_id;
    }

    public String getResults() {
        return results;
    }

    public void setResults(String results) {
        this.results = results;
    }

    public String getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }

    public String getInserted_at() {
        return inserted_at;
    }

    public void setInserted_at(String inserted_at) {
        this.inserted_at = inserted_at;
    }

    public GlificStudentResult getGlificStudentResult() {
        return glificStudentResult;
    }

    public void setGlificStudentResult(GlificStudentResult glificStudentResult) {
        this.glificStudentResult = glificStudentResult;
    }

    @Override
    public String toString() {
        return "GlificStudent{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                ", status='" + status + '\'' +
                ", completed_at='" + completed_at + '\'' +
                ", contact_id='" + contact_id + '\'' +
                ", contact_phone='" + contact_phone + '\'' +
                ", flow_id='" + flow_id + '\'' +
                ", results='" + results + '\'' +
                ", updated_at='" + updated_at + '\'' +
                ", inserted_at='" + inserted_at + '\'' +
                ", glificStudentResult=" + glificStudentResult +
                '}';
    }
}
