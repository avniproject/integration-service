package org.avni_integration_service.lahi.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.avni_integration_service.avni.domain.Subject;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Student implements LahiEntity, StudentConstants {
    private Map<String, Object> response;

    private static final List<String> Core_Fields = Arrays.asList(FIRST_NAME,LAST_NAME,DATE_OF_BIRTH,
    DATE_OF_REGISTRATION,GENDER);
    //TODO add address fields to above and also anyother fields

    public static StudentConstants from(Map<String, Object> studentResponse) {
        Student student = new Student();
        student.response = studentResponse;
        return student;
    }

    public Subject subjectWithoutObservations() {
        Subject subject = new Subject();
        subject.setSubjectType("Student");
        subject.setAddress(STUDENT_ADDRESS);
//        Date demandDate = DateTimeUtil.offsetTimeZone(new Date(), DateTimeUtil.UTC, DateTimeUtil.IST);
//        subject.setRegistrationDate(demandDate);
//        subject.setAddress(MapUtil.getString(DemandStateField, response) +", "+MapUtil.getString(DemandDistrictField, response));
//        subject.setExternalId(MapUtil.getString(DemandIdField, response));
//        subject.setFirstName(MapUtil.getString(DemandNameField, response));
//        subject.setVoided(MapUtil.getBoolean(DemandIsVoidedField, response));
//        String[] arrayOfTCs = MapUtil.getString(DemandTargetCommunity, response) != null ? MapUtil.getString(DemandTargetCommunity, response).split(";") : null;
//        subject.("Target Community", arrayOfTCs);
//        subject.addObservation("Type of Disaster", demandDto.getTypeOfDisaster());
//        subject.addObservation("Number of people", this.getNumberOfPeople());
//        subject.addObservation("Account Name", this.getAccountName());
//        subject.addObservation("AccountId", this.getAccountId());
//        subject.addObservation("DemandId", demandDto.getDemandId());
//        subject.addObservation("demandName", demandDto.getDemandName());
//        subject.addObservation("District", demandDto.getDistrict());
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
