package org.avni_integration_service.lahi.config;

import java.util.Collections;
import java.util.Map;

import static java.util.Map.entry;

public interface LahiMappingDbConstants {
    String MAPPING_TYPE_OBS = "Observations";
    String MAPPING_GROUP_STUDENT = "Student";

    Map<String,Object> DEFAULT_STUDENT_OBS_VALUE_MAP = Map.ofEntries(
            entry("Does student give permission to LAHI to Send Whatsapp/SMS/Call for any career opportunities?","Yes"),
            entry("Career options you are interested in", Collections.singletonList("Other")),
            entry("User","LAHI Program Team"),
            entry("Student/Trainee Type","Regular"),
            entry("Registration Source","Glific"),
            entry("Trade/Sector","Other")
    );
}
