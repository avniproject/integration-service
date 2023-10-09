package org.avni_integration_service.lahi.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.lahi.util.DateTimeUtil;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Date;
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

        String firstName = StringUtils.capitalize(response.get(FIRST_NAME).toString());
        String lastName = StringUtils.capitalize(response.get(LAST_NAME).toString());
        Date registrationDate = DateTimeUtil.registrationDate(response.get(DATE_OF_REGISTRATION).toString());
        Date dob = DateTimeUtil.dateOfBirth(response.get(DATE_OF_BIRTH).toString());
        String gender = response.get(GENDER).toString();
        String external_id = response.get(FLOWRESULT_ID).toString();

        // TODO: 21/09/23 set external id and latter remove after all testing
        subject.setExternalId(external_id);
        subject.setAddress(STUDENT_ADDRESS);
        subject.setFirstName(firstName);
        subject.setLastName(lastName);
        subject.setRegistrationDate(registrationDate);
        subject.setDateOfBirth(dob);
        subject.setGender(gender);
        subject.setSubjectType("Student");
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
