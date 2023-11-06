package org.avni_integration_service.lahi.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.log4j.Logger;
import org.avni_integration_service.glific.bigQuery.domain.FlowResult;

import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LahiStudent implements LahiStudentConstants {
    private static final Logger logger = Logger.getLogger(LahiStudent.class);
    private static final List<String> Core_Fields = Arrays.asList(FIRST_NAME,LAST_NAME,DATE_OF_BIRTH, GENDER);
    private static final List<String> PRIMITIVE_OBS_FIELDS = Arrays.asList(OTHER_STATE, DISTRICT, CITY_NAME, SCHOOL,
//            STUDENT_CONTACT_NUMBER, ALTERNATE_NUMBER,
            EMAIL, OTHER_QUALIFICATION, FATHER_NAME, QUALIFICATION_STREAM);
    private static final List<String> CODED_OBS_FIELDS = Arrays.asList(STATE, HIGHEST_QUALIFICATION,
            QUALIFICATION_STATUS, ACADEMIC_YEAR, VOCATIONAL, TRADE, STREAM);

    private FlowResult flowResult;

    public LahiStudent(FlowResult flowResult) {
        this.flowResult = flowResult;
    }

    public String getContactPhone() {
        return flowResult.getContactPhone();
    }

    public String getAlternatePhoneNumber() {
        return getInput(ALTERNATE_NUMBER);
    }

    public String getFirstName() {
        return getInput(FIRST_NAME);
    }

    public String getLastName() {
        return getInput(LAST_NAME);
    }

    public String getGender() {
        return getCategory(GENDER);
    }

    public String getDateOfRegistration() {
        return getInsertedAt();
    }

    public String getDateOfBirth() {
        return getInput(DATE_OF_BIRTH);
    }

    public String getInput(String key) {
        return flowResult.getInput(key);
    }

    public String getCategory(String key) {
        return flowResult.getCategory(key);
    }

    public String getLastUpdatedAt() {
        return flowResult.getUpdatedAt();
    }

    public String getInsertedAt() {
        return flowResult.getInsertedAt();
    }

    public String getFlowResultId() {
        return flowResult.getFlowResultId();
    }

    public Map<String, String> getObservations() {
        HashMap<String, String> observations = new HashMap<>();

        PRIMITIVE_OBS_FIELDS.forEach(fieldName -> observations.put(fieldName, getInput(fieldName)));
        CODED_OBS_FIELDS.forEach(fieldName -> observations.put(fieldName, getCategory(fieldName)));

        return observations;
    }
}
