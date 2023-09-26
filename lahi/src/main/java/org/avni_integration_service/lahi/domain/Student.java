package org.avni_integration_service.lahi.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.lahi.util.DateTimeUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Student implements LahiEntity, StudentConstants {
    //todo organise methods to below attribute declarations
    public Map<String, Object> getResponse() {
        return response;
    }

    public void setResponse(Map<String, Object> response) {
        this.response = response;
    }

    private Map<String, Object> response;
    private static final Logger logger = Logger.getLogger(Student.class);

    private static final List<String> Core_Fields = Arrays.asList(FIRST_NAME,LAST_NAME,DATE_OF_BIRTH,
    DATE_OF_REGISTRATION,GENDER);
    //TODO add address fields to above and also anyother fields

    public static Student from(Map<String, Object> studentResponse) {
        Student student = new Student();
        student.response = studentResponse;
        return student;
    }


    public Subject subjectWithoutObservations() {
        Subject subject = new Subject();
        subject.setSubjectType("Student");
        subject.setAddress(STUDENT_ADDRESS);
        // TODO: 21/09/23 set external id and latter remove after all testing
        subject.setExternalId("12345");
        subject.setFirstName(response.get(FIRST_NAME).toString());
        subject.setLastName(response.get(LAST_NAME).toString());
        subject.setRegistrationDate(DateTimeUtil.registrationDate(response.get(DATE_OF_REGISTRATION).toString()));
        subject.setDateOfBirth(DateTimeUtil.dateOfBirth(response.get(DATE_OF_BIRTH).toString()));
        subject.setGender(response.get(GENDER).toString());
        return subject;
    }



    @Override
    public List<String> getObservationFields() {
        return response.keySet().stream().filter(s -> !Core_Fields.contains(s)).collect(Collectors.toList());
    }

    @Override
    public Object getValue(String responseField) {
        return this.response.get(responseField);
    }
}
