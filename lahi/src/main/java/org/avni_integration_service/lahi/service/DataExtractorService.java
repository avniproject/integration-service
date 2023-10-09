package org.avni_integration_service.lahi.service;

import com.google.cloud.bigquery.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.log4j.Logger;
import org.avni_integration_service.lahi.config.BigQueryConnector;
import org.avni_integration_service.lahi.domain.StudentConstants;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DataExtractorService {

    public static final String RESULTS = "results";
    private final BigQueryConnector bigQueryConnector;
    private static final Logger logger = Logger.getLogger(DataExtractorService.class);

    public DataExtractorService(BigQueryConnector bigQueryConnector) {
        this.bigQueryConnector = bigQueryConnector;
    }


    public TableResult queryWithPagination(String query, String date, int limit) throws InterruptedException {

            QueryJobConfiguration queryConfig =
                    QueryJobConfiguration.newBuilder(query)
                            .addNamedParameter("updated_at",QueryParameterValue.string(date))
                            .addNamedParameter("limit_count",QueryParameterValue.int64(limit))
                            .build();

            TableResult tableResult = queryCall(queryConfig);

        return tableResult;
    }

    public TableResult queryCall(QueryJobConfiguration queryJobConfiguration)throws InterruptedException{
        JobId jobId = JobId.of(UUID.randomUUID().toString());
        Job queryJob = bigQueryConnector.getBigQuery().create(JobInfo.newBuilder(queryJobConfiguration).setJobId(jobId).build());
        queryJob = queryJob.waitFor();

        // Check for errors
        if (queryJob == null) {
            logger.info("query job is null");
            throw new RuntimeException("Job no longer exists");
        } else if (queryJob.getStatus().getError() != null) {
            // You can also look at queryJob.getStatus().getExecutionErrors() for all
            // errors, not just the latest one.
            logger.info(queryJob.getStatus().getError().toString());
            throw new RuntimeException(queryJob.getStatus().getError().toString());
        }

        // Get the results.
        TableResult result = queryJob.getQueryResults();

        return result;


    }


    public List<Map<String,Object>> filterData(TableResult response){
        Schema schema = response.getSchema();
        List<Map<String,Object>> list1 = new LinkedList<>();
        for (FieldValueList row : response.iterateAll()) {
            Map<String, Object> resultMap = new HashMap<>();
            for (int i = 0; i < schema.getFields().size(); i++) {
                Field field = schema.getFields().get(i);
                FieldValue fieldValue = row.get(i);
                String fieldName = field.getName();
                if(fieldName.equals(RESULTS) ){
                    getResultData(resultMap,fieldValue.getStringValue());
                }
                resultMap.put(fieldName,fieldValue.getStringValue());
            }
            list1.add(resultMap);
        }
        return list1;
    }

    public void getResultData(Map<String,Object> map,String result){
        JsonObject jsonObject = new JsonParser().parse(result).getAsJsonObject();
        StudentConstants.ResultFieldList.forEach(field->{
            map.put(field,getDataFromJson(jsonObject,field));
        });
    }


    public String getDataFromJson(JsonObject jsonObject,String field){
        return  (jsonObject.has(field))?jsonObject.getAsJsonObject(field).get("input").getAsString():null;
    }


}
