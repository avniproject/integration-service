package org.avni_integration_service.lahi.domain;

import java.util.Arrays;
import java.util.List;

public interface StudentConstants {

    String FIRST_NAME = "avni_first_name";
    String LAST_NAME = "avni_last_name";
    String DATE_OF_BIRTH = "avni_date_of_birth";
    String DATE_OF_REGISTRATION = "updated_at";
    String GENDER = "avni_gender";
    String STATE = "avni_state";
    String DISTRICT = "avni_district_name";
    String BLOCK = "";
    String SCHOOL = "avni_school_name";
    String UDISE = "";
    String OTHER_SCHOOL_NAME = "";
    String STUDENT_CONTACT_NUMBER = "phone";
    String ALTERNATE_NUMBER = "avni_alternate_contact";
    String EMAIL = "avni_email";
    String HIGHEST_QUALIFICATION = "avni_highest_qualification";
    String OTHER_QUALIFICATION = "avni_other_qualification";
    String QUALIFICATION_STATUS = "avni_qualification_status";
    String ACADEMIC_YEAR = "avni_academic_year";
    String VOCATIONAL = "avni_vocational";
    String TRADE = "avni_trade";
//    String STUDENT_PERMISSION = "";
    String STUDENT_ADDRESS = "Other, Other, Other, Other";
    String AVNI_OPTIN = "avni_optin";

    List<String> ResultFieldList =
            Arrays.asList(
                    FIRST_NAME,
                    LAST_NAME,
                    DATE_OF_BIRTH,
                    GENDER,
                    STATE,
                    DISTRICT,
                    SCHOOL,
                    ALTERNATE_NUMBER,
                    EMAIL,
                    HIGHEST_QUALIFICATION,
                    OTHER_QUALIFICATION,
                    QUALIFICATION_STATUS,
                    ACADEMIC_YEAR,
                    VOCATIONAL,
                    TRADE,
                    AVNI_OPTIN
            );

}
