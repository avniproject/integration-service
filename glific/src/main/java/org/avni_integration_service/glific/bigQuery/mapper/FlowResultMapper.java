package org.avni_integration_service.glific.bigQuery.mapper;

import com.google.cloud.bigquery.*;
import org.avni_integration_service.glific.bigQuery.domain.FlowResult;

import java.util.*;

public class FlowResultMapper implements BigQueryResultMapper<FlowResult> {
    @Override
    public FlowResult map(Schema schema, FieldValueList fieldValues) {
        if (fieldValues == null) return null;

        HashMap<String, Object> fields = schema.getFields().stream()
                .reduce(new HashMap<>(),
                        (hashMap, field) -> {
                            hashMap.merge(field.getName(), fieldValues.get(field.getName()).getValue(), Objects::equals);
                            return hashMap;
                        }, (first, second) -> {first.putAll(second);return first;});
        return new FlowResult(fields);
    }
}
