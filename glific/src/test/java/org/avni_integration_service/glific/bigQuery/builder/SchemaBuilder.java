package org.avni_integration_service.glific.bigQuery.builder;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.Schema;

import static org.avni_integration_service.glific.bigQuery.domain.FlowResult.STUDENT_CONTACT_NUMBER;

public class SchemaBuilder {

    public Schema flowResultSchema() {
        return Schema.of(Field.of(STUDENT_CONTACT_NUMBER, LegacySQLTypeName.STRING),
                Field.of("flowresult_id", LegacySQLTypeName.STRING),
                Field.of("inserted_at", LegacySQLTypeName.DATETIME),
                Field.of("results", LegacySQLTypeName.STRING),
                Field.of("updated_at", LegacySQLTypeName.DATETIME));
    }
}
