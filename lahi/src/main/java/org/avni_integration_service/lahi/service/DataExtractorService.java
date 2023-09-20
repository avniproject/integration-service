package org.avni_integration_service.lahi.service;

import com.google.cloud.bigquery.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.log4j.Logger;
import org.avni_integration_service.lahi.config.BigQueryConnector;
import org.avni_integration_service.lahi.domain.GlificStudent;
import org.avni_integration_service.lahi.domain.GlificStudentResult;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DataExtractorService {

    private final BigQueryConnector bigQueryConnector;

//    private final BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();

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


    public List<Map<String,String>> filterData(TableResult response){
        Schema schema = response.getSchema();
        List<Map<String,String>> list1 = new LinkedList<>();
        for (FieldValueList row : response.iterateAll()) {
            Map<String, String> resultMap1 = new HashMap<>();
            for (int i = 0; i < schema.getFields().size(); i++) {
                Field field = schema.getFields().get(i);
                FieldValue fieldValue = row.get(i);
                String fieldName = field.getName();
                resultMap1.put(fieldName,fieldValue.getStringValue());
            }
            list1.add(resultMap1);
        }
        return list1;
    }


    public List<GlificStudent> mappingToGlificStudentList(List<Map<String,String>> list){
        List<GlificStudent> glificStudentList = new LinkedList<>();
        list.stream().forEach(map->{
            GlificStudent glificStudent = new GlificStudent();
            glificStudent.setName(map.get("name"));
            glificStudent.setId(map.get("id"));
            glificStudent.setStatus(map.get("status"));
            glificStudent.setCompleted_at(map.get("completed_at"));
            glificStudent.setContact_id(map.get("contact_id"));
            glificStudent.setContact_phone(map.get("contact_phone"));
            glificStudent.setFlow_id(map.get("flow_id"));
            glificStudent.setResults(map.get("results"));
            glificStudent.setUpdated_at(map.get("updated_at"));
            glificStudent.setInserted_at(map.get("inserted_at"));
            glificStudentList.add(glificStudent);
        });
        return glificStudentList;
    }

    public GlificStudentResult mapToGlificStudentResult(GlificStudent glificStudent){
        String result = glificStudent.getResults();
        JsonObject jsonObject = new JsonParser().parse(result).getAsJsonObject();

        GlificStudentResult glificStudentResult = new GlificStudentResult();
        glificStudentResult.setAvni_first_name(getDataFromJson(jsonObject,"avni_first_name"));
        glificStudentResult.setAvni_last_name(getDataFromJson(jsonObject,"avni_last_name"));
        glificStudentResult.setAvni_date_of_birth(getDataFromJson(jsonObject,"avni_date_of_birth"));
        glificStudentResult.setAvni_gender(getDataFromJson(jsonObject,"avni_gender"));
        glificStudentResult.setAvni_state(getDataFromJson(jsonObject,"avni_state"));
        glificStudentResult.setAvni_district_name(getDataFromJson(jsonObject,"avni_district_name"));
        glificStudentResult.setAvni_school_name(getDataFromJson(jsonObject,"avni_school_name"));
        glificStudentResult.setAvni_alternate_contact(getDataFromJson(jsonObject,"avni_alternate_contact"));
        glificStudentResult.setAvni_email(getDataFromJson(jsonObject,"avni_email"));
        glificStudentResult.setAvni_highest_qualification(getDataFromJson(jsonObject,"avni_highest_qualification"));
        glificStudentResult.setAvni_other_qualification(getDataFromJson(jsonObject,"avni_other_qualification"));
        glificStudentResult.setAvni_qualification_status(getDataFromJson(jsonObject,"avni_qualification_status"));
        glificStudentResult.setAvni_academic_year(getDataFromJson(jsonObject,"avni_academic_year"));
        glificStudentResult.setAvni_vocational(getDataFromJson(jsonObject,"avni_vocational"));

        return glificStudentResult;

    }

    public String getDataFromJson(JsonObject jsonObject,String field){
        return  (jsonObject.has(field))?jsonObject.getAsJsonObject(field).get("input").getAsString():null;
    }



}
