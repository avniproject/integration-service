package org.avni_integration_service.tanuh.domain;

// Exactly as Tanuh's configuration names them. The server resolves a coded answer by name across the whole
// organisation and does not check it belongs to the question, so a near-miss is written without an error.
public final class TanuhConcepts {
    public static final String ORAL_SCREENING = "Oral Screening";
    public static final String TAKE_PHOTOS = "Take photos of all lesions and 1 photo without lesion";
    public static final String GUIDED_PHOTOS = "ORAL SCREENING";
    public static final String ORAL_IMAGE = "Oral Image";
    public static final String SUSPICIOUS_LESION = "Suspicious Lesion?";
    public static final String YES = "Yes";

    public static final String MODEL_RESULT = "High risk model result";
    public static final String MODEL_STATUS = "High risk model status";
    public static final String MODEL_VERSION = "High risk model version";
    public static final String MODEL_RUN_TIME = "High risk model run time";
    public static final String REVIEW_CATEGORY = "Review category";
    public static final String STATUS_SCORED = "Scored";
    public static final String STATUS_NOT_SCORED = "Not scored";

    private TanuhConcepts() {
    }
}
