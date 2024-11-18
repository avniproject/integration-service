package org.avni_integration_service.goonj.config;

import java.util.Set;

public class GoonjConstants {
    public static final String GoonjErrorRecordLog = "GoonjErrorRecordLog";
    public static final String HEALTHCHECK_SLUG_GOONJ_INTEGRATION = "goonj-integration";
    public static final String HEALTHCHECK_SLUG_GOONJ_SALESFORCE = "goonj-salesforce";
    public static final String EMPTY_STRING = "";
    public static final String API_PARAMS_DELIMITER = "&";
    public static final String FILTER_KEY_ACCOUNT = "account";
    public static final String FILTER_KEY_CONCEPTS = "concepts";
    public static final String FILTER_KEY_LOCATION_IDS = "locationIds";
    public static final String FILTER_KEY_STATE = "state";
    public static final String FILTER_KEY_TIMESTAMP = "dateTimestamp";
    public static final String FILTER_PARAM_FORMAT = "%s=%s";
    public static final boolean UPDATE_SYNC_STATUS_GOONJ_MAIN_JOB = true;
    public static final boolean UPDATE_SYNC_STATUS_GOONJ_ADHOC_JOB = false;
    public static final Set<String> GOONJ_API_FILTER = Set.of(FILTER_KEY_ACCOUNT,FILTER_KEY_STATE);
    public static final Set<String> AVNI_API_FILTER = Set.of(FILTER_KEY_CONCEPTS,FILTER_KEY_LOCATION_IDS);
}
