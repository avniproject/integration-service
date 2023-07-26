package org.avni_integration_service.web.contract;

public class BaseEntityContract {
    private int id;

    private String uuid;

    private boolean isVoided;


    public boolean isVoided() {
        return isVoided;
    }

    public void setVoided(boolean voided) {
        isVoided = voided;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public BaseEntityContract(int id) {
        this.id = id;
    }

    public BaseEntityContract(int id, String uuid) {
        this.id = id;
        this.uuid = uuid;
    }

    public BaseEntityContract() {
    }
}
