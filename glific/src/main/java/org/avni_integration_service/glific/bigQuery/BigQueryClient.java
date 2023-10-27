package org.avni_integration_service.glific.bigQuery;

import com.google.cloud.bigquery.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.log4j.Logger;
import org.avni_integration_service.glific.bigQuery.config.BigQueryConnector;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class BigQueryClient {
    public static final String RESULTS = "results";
    private final BigQueryConnector bigQueryConnector;
    private static final Logger logger = Logger.getLogger(BigQueryClient.class);

    public BigQueryClient(BigQueryConnector bigQueryConnector) {
        this.bigQueryConnector = bigQueryConnector;
    }

    public List<Map<String, Object>> queryWithPagination(String query, String date, int limit, List<String> fields) {
        QueryJobConfiguration queryConfig =
                QueryJobConfiguration.newBuilder(query)
                        .addNamedParameter("updated_at", QueryParameterValue.string(date))
                        .addNamedParameter("limit_count", QueryParameterValue.int64(limit))
                        .build();
        TableResult tableResult = queryCall(queryConfig);
        return this.filterData(tableResult, fields);
    }

    private TableResult queryCall(QueryJobConfiguration queryJobConfiguration) {
        try {
            JobId jobId = JobId.of(UUID.randomUUID().toString());
            Job queryJob = bigQueryConnector.getBigQuery().create(JobInfo.newBuilder(queryJobConfiguration).setJobId(jobId).build());
            queryJob = queryJob.waitFor();

            if (queryJob == null) {
                logger.info("query job is null");
                throw new RuntimeException("Job no longer exists");
            } else if (queryJob.getStatus().getError() != null) {
                // You can also look at queryJob.getStatus().getExecutionErrors() for all
                // errors, not just the latest one.
                logger.info(queryJob.getStatus().getError().toString());
                throw new RuntimeException(queryJob.getStatus().getError().toString());
            }

            return queryJob.getQueryResults();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Map<String, Object>> filterData(TableResult response, List<String> resultFields) {
        Schema schema = response.getSchema();
        List<Map<String, Object>> list1 = new LinkedList<>();
        for (FieldValueList row : response.iterateAll()) {
            Map<String, Object> resultMap = new HashMap<>();
            for (int i = 0; i < schema.getFields().size(); i++) {
                Field field = schema.getFields().get(i);
                FieldValue fieldValue = row.get(i);
                String fieldName = field.getName();
                if (fieldName.equals(RESULTS)) {
                    getResultData(resultMap, fieldValue.getStringValue(), resultFields);
                }
                resultMap.put(fieldName, fieldValue.getStringValue());
            }
            list1.add(resultMap);
        }
        return list1;
    }

    private void getResultData(Map<String, Object> map, String result, List<String> resultFields) {
        JsonObject jsonObject = new JsonParser().parse(result).getAsJsonObject();
        resultFields.forEach(field -> {
            map.put(field, getDataFromJson(jsonObject, field));
        });
    }

    private String getDataFromJson(JsonObject jsonObject, String field) {
        return (jsonObject.has(field)) ? jsonObject.getAsJsonObject(field).get("input").getAsString() : null;
    }
}
