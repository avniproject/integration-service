package org.avni_integration_service.lahi.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.util.MapUtil;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Student implements LahiEntity {

    private static final String FIRST_NAME = "avni_first_name";
    private static final String LAST_NAME = "avni_last_name";
    private static final String DATE_OF_BIRTH = "avni_date_of_birth";
    private static final String DATE_OF_REGISTRATION = "updated_at";
    private static final String GENDER = "avni_gender";
    private static final String STATE = "avni_state";
    private static final String DISTRICT = "avni_district_name";
    private static final String BLOCK = "";
    private static final String SCHOOL = "avni_school_name";
    private static final String UDISE = "";
    private static final String OTHER_SCHOOL_NAME = "";
    private static final String STUDENT_CONTACT_NUMBER = "phone";
    private static final String ALTERNATE_NUMBER = "avni_alternate_contact";
    private static final String EMAIL = "avni_email";
    private static final String HIGHEST_QUALIFICATION = "avni_highest_qualification";
    private static final String OTHER_QUALIFICATION = "avni_other_qualification";
    private static final String QUALIFICATION_STATUS = "avni_qualification_status";
    private static final String ACADEMIC_YEAR = "avni_academic_year";
    private static final String VOCATIONAL = "avni_vocational";
    private static final String TRADE = "avni_trade";
    private static final String STUDENT_PERMISSION = "";
    private static final String STUDENT_ADDRESS = "Other, Other, Other, Other";

    //TODO remove demand field start

    private static final String DemandDistrictField = "District";
    private static final String DemandStateField = "State";
    private static final String DemandNameField = "DemandName";
    private static final String DemandTargetCommunity = "TargetCommunity";
    private static final String DemandIdField = "DemandId";
    private static final String DemandIsVoidedField = "IsVoided";
    private static final String DemandStatusField = "DemandStatus";
    private static final String DispatchStatusField = "DispatchStatus";

    //TODO remove demand field end
    private Map<String, Object> response;

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
//        Date demandDate = DateTimeUtil.offsetTimeZone(new Date(), DateTimeUtil.UTC, DateTimeUtil.IST);
//        subject.setRegistrationDate(demandDate);
//        subject.setAddress(MapUtil.getString(DemandStateField, response) +", "+MapUtil.getString(DemandDistrictField, response));
//        subject.setExternalId(MapUtil.getString(DemandIdField, response));
//        subject.setFirstName(MapUtil.getString(DemandNameField, response));
//        subject.setVoided(MapUtil.getBoolean(DemandIsVoidedField, response));
//        String[] arrayOfTCs = MapUtil.getString(DemandTargetCommunity, response) != null ? MapUtil.getString(DemandTargetCommunity, response).split(";") : null;
//        subject.addObservation("Target Community", arrayOfTCs);
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
