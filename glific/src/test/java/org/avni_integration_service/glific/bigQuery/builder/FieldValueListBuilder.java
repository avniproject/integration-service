package org.avni_integration_service.glific.bigQuery.builder;

import com.google.cloud.bigquery.*;

import java.util.Arrays;

public class FieldValueListBuilder {
    public FieldValueList buildFlowResult(String contactPhone, String flowResultId, String results) {
        return FieldValueList.of(Arrays.asList(
                        FieldValue.of(FieldValue.Attribute.PRIMITIVE, contactPhone),
                        FieldValue.of(FieldValue.Attribute.PRIMITIVE, flowResultId),
                        FieldValue.of(FieldValue.Attribute.PRIMITIVE, "2023-08-02T14:21:25.695844"),
                        FieldValue.of(FieldValue.Attribute.PRIMITIVE, results),
                        FieldValue.of(FieldValue.Attribute.PRIMITIVE, "2023-08-02T14:21:25.695844")),
                new SchemaBuilder().flowResultSchema().getFields());
    }
}
