package org.avni_integration_service.lahi.domain;

import java.util.Arrays;
import java.util.List;

public interface StudentConstants {

    String FIRST_NAME = "avni_first_name";
    String LAST_NAME = "avni_last_name";
    String DATE_OF_BIRTH = "avni_date_of_birth";
    String DATE_OF_REGISTRATION = "inserted_at";
    String GENDER = "avni_gender";
    String STATE = "avni_state";
    String OTHER_STATE = "avni_other_state";
    String DISTRICT = "avni_district_name";
    String CITY_NAME = "avni_city_name";
    String SCHOOL = "avni_school_name";
    String STUDENT_CONTACT_NUMBER = "contact_phone";
    String ALTERNATE_NUMBER = "avni_alternate_contact";
    String EMAIL = "avni_email";
    String HIGHEST_QUALIFICATION = "avni_highest_qualification";
    String OTHER_QUALIFICATION = "avni_other_qualification";
    String QUALIFICATION_STATUS = "avni_qualification_status";
    String ACADEMIC_YEAR = "avni_academic_year";
    String VOCATIONAL = "avni_vocational";
    String TRADE = "avni_trade";
    String STUDENT_ADDRESS = "Other, Other, Other, Other";
    String AVNI_OPTIN = "avni_optin";
    String FATHER_NAME = "avni_father_name";
    String STREAM = "avni_stream";
    String QUALIFICATION_STREAM = "avni_other_qualification_stream";

    List<String> ResultFieldList =
            Arrays.asList(
                    FIRST_NAME,
                    LAST_NAME,
                    FATHER_NAME,
                    DATE_OF_BIRTH,
                    GENDER,
                    STATE,
                    OTHER_STATE,
                    DISTRICT,
                    CITY_NAME,
                    SCHOOL,
                    ALTERNATE_NUMBER,
                    EMAIL,
                    HIGHEST_QUALIFICATION,
                    OTHER_QUALIFICATION,
                    QUALIFICATION_STATUS,
                    ACADEMIC_YEAR,
                    VOCATIONAL,
                    TRADE,
                    AVNI_OPTIN,
                    STREAM,
                    QUALIFICATION_STREAM
            );

    List<String> MandatoryField =
            Arrays.asList(
                    FIRST_NAME,
                    LAST_NAME,
                    DATE_OF_BIRTH,
                    GENDER,
                    FATHER_NAME,
                    STUDENT_CONTACT_NUMBER,
                    HIGHEST_QUALIFICATION,
                    ACADEMIC_YEAR,
                    QUALIFICATION_STREAM,
                    OTHER_QUALIFICATION,
                    VOCATIONAL
            );

}
