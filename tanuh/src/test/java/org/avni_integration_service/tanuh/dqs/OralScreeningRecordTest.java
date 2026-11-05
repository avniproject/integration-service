package org.avni_integration_service.tanuh.dqs;

import org.avni_integration_service.tanuh.domain.TanuhConcepts;
import org.avni_integration_service.tanuh.domain.TanuhConceptsFixture;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import static org.avni_integration_service.tanuh.dqs.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

public class OralScreeningRecordTest {
    private static final String SPLIT_GROUP = "Take photos of all lesions and 1 photo without lesion";
    private static final String GUIDED_GROUP = "ORAL SCREENING";
    private static final String AI_GROUP = "Image-wise AI Assessment";
    private static final UnaryOperator<String> SIGN = url -> url + "?X-Amz-Signature=signed";

    private static GeneralEncounter fullScreening() {
        GeneralEncounter s = screening("s1");
        s.addObservation("Cigarettes / Bidi Usage", "Never");
        s.addObservation("Smokeless Tobacco Usage", "Current");
        s.addObservation("Areca Nut Consumption", "Past");
        s.addObservation("Alcohol Consumption", "Current");
        s.addObservation("Frequency of Alcohol Consumption", "1–2 days per week");
        s.addObservation("Any Symptoms", "No");
        s.addObservation("Pre-capture instructions acknowledged", "Yes");
        s.addObservation("Able to Open Mouth?", "Yes");
        s.addObservation("Do you see any lesions?", "Yes");
        s.addObservation("App found No Suspicious Lesion. But you noticed suspicious lesions. Would you like to refer the patient to a dentist?", "Yes");
        // The server answers a Location with the facility's levels in order, the facility itself first.
        Map<String, Object> place = new LinkedHashMap<>();
        place.put("Taluka Hospital", "Hosakote Taluka Hospital");
        place.put("District", "Bengaluru Rural");
        s.addObservation("Place of referral", place);
        s.addObservation(SPLIT_GROUP, rows(
                photoRow("https://s3/t/a.jpg", "Yes"),
                photoRow("https://s3/t/b.jpg", "No"),
                photoRow("https://s3/t/c.jpg", "Yes"),
                photoRow("https://s3/t/d.jpg", null)));
        s.addObservation(AI_GROUP, rows(aiRow("Suspicious"), aiRow("Suspicious"), aiRow("Non-Suspicious"), aiRow(null)));
        return s;
    }

    // "Not suspicious" is the mapped answer, not anything starting with "non".
    @Test
    public void onlyTheMappedAnswersCountAsNotSuspicious() {
        GeneralEncounter s = screening("s3");
        s.addObservation(SPLIT_GROUP, rows(photoRow("https://s3/t/a.jpg", "No"), photoRow("https://s3/t/b.jpg", "No"), photoRow("https://s3/t/c.jpg", "No")));
        s.addObservation(AI_GROUP, rows(aiRow("Non-Suspicious"), aiRow("Non Suspicious"), aiRow("None")));

        Map<String, Object> record = OralScreeningRecord.from(s, patient(), TanuhConceptsFixture.defaults(), SIGN);

        assertEquals(2, record.get("images_neither_suspicious"), "the current and the older spelling");
        assertEquals(1, record.get("images_ai_missing"), "'None' is no verdict");
    }

    @Test
    public void aRenamedNotSuspiciousAnswerIsReadThroughTheMapping() {
        GeneralEncounter s = screening("s4");
        s.addObservation(SPLIT_GROUP, rows(photoRow("https://s3/t/a.jpg", "No")));
        s.addObservation(AI_GROUP, rows(aiRow("Clear")));
        TanuhConcepts renamed = TanuhConceptsFixture.editedBy(rows -> {
            rows.replaceAll(row -> row.key().equals("non_suspicious") ? new TanuhConcepts.Mapping("non_suspicious", "Clear") : row);
            return rows;
        });

        Map<String, Object> record = OralScreeningRecord.from(s, patient(), renamed, SIGN);

        assertEquals(1, record.get("images_neither_suspicious"));
        assertEquals(0, record.get("images_ai_missing"));
    }

    @Test
    public void theEnvelopeComesFromTheVisitItsAuditAndThePatient() {
        Map<String, Object> record = OralScreeningRecord.from(fullScreening(), patient(), TanuhConceptsFixture.defaults(), SIGN);

        assertEquals("s1", record.get("record_id"));
        assertEquals("patient-1", record.get("beneficiary_id"));
        assertEquals(WORKER, record.get("worker_id"));
        assertEquals("625529", record.get("village_id"), "the village's LGD code, not its Avni uuid");
        assertEquals(VISIT_AT, record.get("visit_at"));
        assertEquals(CREATED_AT, record.get("recorded_at"));
        assertEquals(MODIFIED_AT, record.get("received_at"));
        assertEquals(new BigDecimal("13.0712"), record.get("gps_lat"));
        assertEquals(new BigDecimal("77.7981"), record.get("gps_lon"));
    }

    @Test
    public void theAnswersAreMappedToTheServicesFields() {
        Map<String, Object> record = OralScreeningRecord.from(fullScreening(), patient(), TanuhConceptsFixture.defaults(), SIGN);

        assertEquals("never", record.get("bidi_cigarette"));
        assertEquals("current", record.get("smokeless_tobacco"));
        assertEquals("past", record.get("areca_nut"));
        assertEquals("current", record.get("alcohol"));
        assertEquals("1–2 days per week", record.get("alcohol_frequency"));
        assertEquals(false, record.get("any_symptoms"));
        assertEquals(true, record.get("precapture_ack"));
        assertEquals(true, record.get("able_to_open_mouth"));
        assertEquals(true, record.get("flw_sees_lesion"));
        assertEquals(true, record.get("flw_overrides_ai"));
        assertEquals("Hosakote Taluka Hospital", record.get("place_of_referral_id"));
        assertFalse(record.containsKey("screening_not_required"), "the under-30 question was not asked");
    }

    @Test
    public void thePhotoCountsPairTheWorkersAnswerWithTheModelsVerdictByRow() {
        Map<String, Object> record = OralScreeningRecord.from(fullScreening(), patient(), TanuhConceptsFixture.defaults(), SIGN);

        assertEquals(4, record.get("image_count"));
        assertEquals(1, record.get("images_ai_missing"));
        assertEquals(1, record.get("images_rater_missing"));
        assertEquals(1, record.get("images_rater_and_ai_suspicious"));
        assertEquals(1, record.get("images_rater_only_suspicious"));
        assertEquals(1, record.get("images_ai_only_suspicious"));
        assertEquals(0, record.get("images_neither_suspicious"));
    }

    @Test
    public void eachPhotoIsSentAsASignedLinkInTheFormsOrder() {
        Map<String, Object> record = OralScreeningRecord.from(fullScreening(), patient(), TanuhConceptsFixture.defaults(), SIGN);

        assertEquals(List.of("https://s3/t/a.jpg?X-Amz-Signature=signed", "https://s3/t/b.jpg?X-Amz-Signature=signed",
                "https://s3/t/c.jpg?X-Amz-Signature=signed", "https://s3/t/d.jpg?X-Amz-Signature=signed"), record.get("image_urls"));
    }

    @Test
    public void aScreeningWithoutPhotosSendsNoLinks() {
        Map<String, Object> record = OralScreeningRecord.from(screening("s8"), patient(), TanuhConceptsFixture.defaults(), SIGN);

        assertEquals(List.of(), record.get("image_urls"));
    }

    @Test
    public void theGuidedGroupIsReadWhenTheLesionGroupIsEmptyAndTheLegacyVerdictChildCounts() {
        GeneralEncounter s = screening("s2");
        s.addObservation(SPLIT_GROUP, List.of());
        Map<String, Object> row = photoRow("https://s3/t/a.jpg", null);
        row.put("Health Worker verdict", "Non-Suspicious");
        row.put("AI verdict", "Non-Suspicious");
        s.addObservation(GUIDED_GROUP, rows(row, photoRow("https://s3/t/b.jpg", "No")));
        s.addObservation(AI_GROUP, rows(aiRow(null), aiRow("Non-Suspicious"), aiRow("Suspicious")));

        Map<String, Object> record = OralScreeningRecord.from(s, patient(), TanuhConceptsFixture.defaults(), SIGN);

        assertEquals(2, record.get("image_count"), "a stale trailing AI row is not a photo");
        assertEquals(0, record.get("images_ai_missing"));
        assertEquals(0, record.get("images_rater_missing"));
        assertEquals(2, record.get("images_neither_suspicious"));
    }

    @Test
    public void aScreeningInTheFlatLayoutCountsItsPhotoSlots() {
        GeneralEncounter s = screening("s3");
        s.addObservation("Photo 1 (image)", "https://s3/t/p1.jpg");
        s.addObservation("Photo 1 — Health Worker verdict", "Suspicious");
        s.addObservation("Photo 1 — AI verdict", "Non-Suspicious");
        s.addObservation("Photo 3 (image)", "https://s3/t/p3.jpg");
        s.addObservation("Photo 3 — ASHA verdict", "Non Suspicious");

        Map<String, Object> record = OralScreeningRecord.from(s, patient(), TanuhConceptsFixture.defaults(), SIGN);

        assertEquals(2, record.get("image_count"));
        assertEquals(1, record.get("images_ai_missing"));
        assertEquals(0, record.get("images_rater_missing"));
        assertEquals(1, record.get("images_rater_only_suspicious"));
    }

    @Test
    public void aScreeningNotProceededWithUnderThirtyIsNotRequiredAndHasNoPhotos() {
        GeneralEncounter s = screening("s4");
        s.addObservation("Oral Cancer Screening is not required as the person is below 30 years of age. Would you still like to proceed?", "No");

        Map<String, Object> record = OralScreeningRecord.from(s, patient(), TanuhConceptsFixture.defaults(), SIGN);

        assertEquals(true, record.get("screening_not_required"));
        assertEquals(0, record.get("image_count"));
        assertEquals(0, record.get("images_neither_suspicious"));
        assertFalse(record.containsKey("flw_sees_lesion"));
    }

    @Test
    public void proceedingUnderThirtyMeansTheScreeningWasRequiredAfterAll() {
        GeneralEncounter s = screening("s5");
        s.addObservation("Oral Cancer Screening is not required as the person is below 30 years of age. Would you still like to proceed?", "Yes");
        assertEquals(false, OralScreeningRecord.from(s, patient(), TanuhConceptsFixture.defaults(), SIGN).get("screening_not_required"));
    }

    @Test
    public void anUnresolvedReferralPlaceIsSentAsItsUuidAndOddValuesAreLeftOut() {
        GeneralEncounter s = screening("s6");
        s.addObservation("Place of referral", "4a43f83e-26db-40c8-83d8-4317dcfda913");
        s.addObservation("Any Symptoms", "Maybe");
        s.addObservation("Smokeless Tobacco Usage", List.of());
        s.set("Encounter Coordinates", "not a point");
        s.set("Encounter date time", "yesterday");

        Map<String, Object> record = OralScreeningRecord.from(s, patient(), TanuhConceptsFixture.defaults(), SIGN);

        assertEquals("4a43f83e-26db-40c8-83d8-4317dcfda913", record.get("place_of_referral_id"));
        assertFalse(record.containsKey("any_symptoms"));
        assertFalse(record.containsKey("smokeless_tobacco"));
        assertFalse(record.containsKey("gps_lat"));
        assertFalse(record.containsKey("visit_at"));
    }

    @Test
    public void aVisitTimeWithAnOffsetIsPassedThroughUnchanged() {
        GeneralEncounter s = screening("s7");
        s.set("Encounter date time", "2026-10-06T11:00:00.000+05:30");
        assertEquals("2026-10-06T11:00:00.000+05:30", OralScreeningRecord.from(s, patient(), TanuhConceptsFixture.defaults(), SIGN).get("visit_at"));
    }
}
