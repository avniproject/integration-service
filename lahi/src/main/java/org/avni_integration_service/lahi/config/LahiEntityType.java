package org.avni_integration_service.lahi.config;

public enum LahiEntityType {
    Student("Student");

    final String dbName;

    LahiEntityType(String dbName) {
        this.dbName = dbName;
    }

    public String getDbName() {
        return dbName;
    }
}
