package org.avni_integration_service.lahi.config;

import org.avni_integration_service.integration_data.domain.framework.IntegrationEntityType;

public enum LahiEntityType implements IntegrationEntityType {
    Student("Student");

    final String dbName;

    LahiEntityType(String dbName) {
        this.dbName = dbName;
    }

    public String getDbName() {
        return dbName;
    }
}
