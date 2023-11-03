package org.avni_integration_service.glific.bigQuery.mapper;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Schema;

public interface BigQueryResultMapper<T> {
    T map(Schema schema, FieldValueList fieldValues);
}
