package org.avni_integration_service.tanuh.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TanuhNamesTest {
    @Test
    public void conceptNamesAreTheOnesTanuhsConfigurationCreates() {
        assertEquals(List.of("High risk model result", "High risk model status", "High risk model version",
                        "High risk model run time", "Review category", "Scored", "Not scored"),
                List.of(TanuhConcepts.MODEL_RESULT, TanuhConcepts.MODEL_STATUS, TanuhConcepts.MODEL_VERSION,
                        TanuhConcepts.MODEL_RUN_TIME, TanuhConcepts.REVIEW_CATEGORY, TanuhConcepts.STATUS_SCORED,
                        TanuhConcepts.STATUS_NOT_SCORED));
    }

    @Test
    public void reviewGroupAnswersAreTheOnesTanuhsConfigurationCreates() {
        assertEquals(List.of("High Risk", "Low Risk", "FLW override", "Not scored", "Safety sample", "Closed"),
                Arrays.stream(ReviewCategory.values()).map(ReviewCategory::getAnswer).toList());
    }

    @Test
    public void modelResultAnswersAreTheOnesTanuhsConfigurationCreates() {
        assertEquals(List.of("High Risk", "Low Risk", "Non Suspicious"),
                Arrays.stream(ModelResult.Result.values()).map(ModelResult.Result::getAnswer).toList());
    }

    @Test
    public void theFormsQuestionsAreNamedAsOnTanuhsForms() {
        assertEquals(List.of("Oral Screening", "Take photos of all lesions and 1 photo without lesion", "ORAL SCREENING",
                        "Oral Image", "Suspicious Lesion?", "Yes"),
                List.of(TanuhConcepts.ORAL_SCREENING, TanuhConcepts.TAKE_PHOTOS, TanuhConcepts.GUIDED_PHOTOS,
                        TanuhConcepts.ORAL_IMAGE, TanuhConcepts.SUSPICIOUS_LESION, TanuhConcepts.YES));
    }
}
