package org.avni_integration_service.lahi.config;

import java.util.Arrays;
import java.util.Map;

import static java.util.Map.entry;

public interface LahiMappingDbConstants {
    String MAPPINGTYPE_OBS = "Observations";
    String MAPPINGGROUP_STUDENT = "Student";

    String CONTACT_PHONE_NUMBER = "Student contact number";
    String ALTERNATE_PHONE_NUMBER = "Alternate (Whatsapp number)";

    //todo create constants for all string literals below
    Map<String,Object> DEFAUL_STUDENT_OBSVALUE_MAP = Map.ofEntries(
            entry("Does student give permission to LAHI to Send Whatsapp/SMS/Call for any career opportunities?","Yes"),
            entry("Career options you are interested in", Arrays.asList("Other")),
            entry("User","LAHI Program Team"),
            entry("Student/Trainee Type","Regular")
    );
}
