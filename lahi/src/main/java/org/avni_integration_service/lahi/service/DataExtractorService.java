package org.avni_integration_service.lahi.service;

import com.google.cloud.bigquery.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.ObservationHolder;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.integration_data.domain.MappingMetaData;
import org.avni_integration_service.integration_data.domain.framework.MappingException;
import org.avni_integration_service.lahi.config.BigQueryConnector;
import org.avni_integration_service.lahi.domain.LahiEntity;
import org.avni_integration_service.lahi.domain.Student;
import org.avni_integration_service.lahi.domain.StudentConstants;
import org.avni_integration_service.util.ObsDataType;
import org.springframework.stereotype.Service;
import org.avni_integration_service.integration_data.repository.MappingMetaDataRepository;
import org.avni_integration_service.lahi.config.LahiMappingDbConstants;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class DataExtractorService {

    public static final String RESULTS = "results";
    private final BigQueryConnector bigQueryConnector;
    private final MappingMetaDataRepository mappingMetaDataRepository;
    private static final Logger logger = Logger.getLogger(DataExtractorService.class);

    public DataExtractorService(BigQueryConnector bigQueryConnector, MappingMetaDataRepository mappingMetaDataRepository) {
        this.bigQueryConnector = bigQueryConnector;
        this.mappingMetaDataRepository = mappingMetaDataRepository;
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

    public boolean validateMandatoryField(Map<String,Object> map){
           long count =  StudentConstants.MandatoryField.stream().filter(field->{
               if(map.getOrDefault(field,null)==null){
                   logger.error(String.format("%s missing for id:%s",field,map.get("id")));
                   return false;
               }
               return true;
           }).count();
           return count == 0;
    }


    public  void populateObservations(ObservationHolder observationHolder, LahiEntity lahiEntity, String mappingGroup) {
        List<String> observationFields = lahiEntity.getObservationFields();
        for (String obsField : observationFields) {
            MappingMetaData mapping = mappingMetaDataRepository.getAvniMappingIfPresent(mappingGroup, LahiMappingDbConstants.MAPPINGTYPE_OBS, obsField, 5);
            if(mapping == null) {
                logger.error("Mapping entry not found for observation field: " + obsField);
                continue;
            }
            ObsDataType dataTypeHint = mapping.getDataTypeHint();
            if (dataTypeHint == null)
                observationHolder.addObservation(mapping.getAvniValue(), lahiEntity.getValue(obsField));
            else if (dataTypeHint == ObsDataType.Coded && lahiEntity.getValue(obsField) != null) {
                MappingMetaData answerMapping = mappingMetaDataRepository.getAvniMappingIfPresent(mappingGroup, LahiMappingDbConstants.MAPPINGTYPE_OBS, lahiEntity.getValue(obsField).toString(), 5);
                if(answerMapping == null) {
                    String errorMessage = "Answer Mapping entry not found for coded concept answer field: " + obsField;
                    logger.error(errorMessage);
                    throw new MappingException(errorMessage);
                }
                observationHolder.addObservation(mapping.getAvniValue(), answerMapping.getAvniValue());
            }
        }
        // TODO: 22/09/23 Handle Other qualifications and Other qualification stream 
    }

    public void setOtherObservation(Subject subject,Student student){
        Map<String,Object> observations = subject.getObservations();
        LahiMappingDbConstants.DEFAUL_STUDENT_OBSVALUE_MAP.forEach(observations::put);
        setOtherAddress(subject, student);
        setPhoneNumber(subject, student);
    }

    public void setOtherAddress(Subject subject, Student student){
        Map<String,Object> subjectObservations = subject.getObservations();
        Map<String,Object> studentResponse = student.getResponse();
        StringBuilder stringBuilder = new StringBuilder();
        setAddressString(stringBuilder,(String)studentResponse.getOrDefault(StudentConstants.STATE,""));
        setAddressString(stringBuilder,(String)studentResponse.getOrDefault(StudentConstants.OTHER_STATE,""));
        setAddressString(stringBuilder,(String)studentResponse.getOrDefault(StudentConstants.DISTRICT,""));
        setAddressString(stringBuilder,(String)studentResponse.getOrDefault(StudentConstants.CITY_NAME,""));
        setAddressString(stringBuilder,(String)studentResponse.getOrDefault(StudentConstants.SCHOOL,""));
        if(stringBuilder.length()>0){
            subjectObservations.put("Other School name",stringBuilder.toString());
        }
    }

    public void setAddressString(StringBuilder stringBuilder, String string){
        if(string != null && !string.equals("")){
            stringBuilder.append(string+" ");
        }
    }

    public void setPhoneNumber(Subject subject, Student student) {
        Map<String, Object> subjectObservations = subject.getObservations();
        String contactPhoneNumber = null;
        if (student.getValue(StudentConstants.STUDENT_CONTACT_NUMBER) != null
                && student.getValue(StudentConstants.STUDENT_CONTACT_NUMBER).toString().length() == 12) {
            contactPhoneNumber = (String) student.getValue(StudentConstants.STUDENT_CONTACT_NUMBER);
            contactPhoneNumber = contactPhoneNumber.substring(2);
            subjectObservations.put(LahiMappingDbConstants.CONTACT_PHONE_NUMBER, contactPhoneNumber);
        }
        setAlternatePhoneNumber(student, subjectObservations, contactPhoneNumber);
    }

    private void setAlternatePhoneNumber(Student student, Map<String, Object> subjectObservations, String contactPhoneNumber) {
        //todo if alternateContactNo
        // a. is a valid number and 12 or 10 digit number then set to 10 digit number
        // b. else set to CONTACT_PHONE_NUMBER
        Long alternatePhoneNumber = null;
        String alternateNumber = (String) student.getValue(StudentConstants.ALTERNATE_NUMBER);
        if (StringUtils.hasText(alternateNumber) && alternateNumber.length() == 12) {
            alternateNumber = alternateNumber.substring(2);
        }
        try {
            alternatePhoneNumber = Long.parseLong((StringUtils.hasText(alternateNumber) && alternateNumber.length() == 10) ?
                            alternateNumber : contactPhoneNumber);
            subjectObservations.put(LahiMappingDbConstants.ALTERNATE_PHONE_NUMBER, alternatePhoneNumber);
        } catch (NumberFormatException nfe) {
            // TODO: 22/09/23  
        }
    }
}
