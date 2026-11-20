package org.avni_integration_service.tanuh.domain;

import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class OralScreeningInputsTest {
    private static Map<String, Object> row(Object image, Object suspicious) {
        Map<String, Object> row = new HashMap<>();
        if (image != null) row.put("Oral Image", image);
        if (suspicious != null) row.put("Suspicious Lesion?", suspicious);
        return row;
    }

    private static GeneralEncounter screening(Object takePhotos, Object guidedPhotos) {
        GeneralEncounter s = new GeneralEncounter();
        if (takePhotos != null) s.addObservation("Take photos of all lesions and 1 photo without lesion", takePhotos);
        if (guidedPhotos != null) s.addObservation("ORAL SCREENING", guidedPhotos);
        s.addObservation("No referral required as app found no suspicious lesions. Do you still want to refer?", "Yes");
        return s;
    }

    @Test
    public void everyRowsPhotoIsRead() {
        OralScreeningInputs inputs = OralScreeningInputs.from(screening(List.of(row("u1", "No"), row("u2", "Yes")), null), TanuhConceptsFixture.defaults());
        assertEquals(List.of("u1", "u2"), inputs.getPhotoUrls());
    }

    @Test
    public void theGuidedGroupIsUsedWhenTakePhotosHasNoRows() {
        OralScreeningInputs inputs = OralScreeningInputs.from(screening(List.of(), List.of(row("g1", "No"), row("g2", "Yes"))), TanuhConceptsFixture.defaults());
        assertEquals(List.of("g1", "g2"), inputs.getPhotoUrls());
    }

    @Test
    public void takePhotosAloneCountsWhenBothGroupsHaveRows() {
        OralScreeningInputs inputs = OralScreeningInputs.from(screening(List.of(row("t1", "No")), List.of(row("g1", "Yes"))), TanuhConceptsFixture.defaults());
        assertEquals(List.of("t1"), inputs.getPhotoUrls());
    }

    @Test
    public void listValuesAreReadAsWellAsSingleOnes() {
        OralScreeningInputs inputs = OralScreeningInputs.from(screening(List.of(row(List.of("u1", "u2"), List.of("Yes"))), null), TanuhConceptsFixture.defaults());
        assertEquals(List.of("u1", "u2"), inputs.getPhotoUrls());
    }

    @Test
    public void rowsWithoutAPhotoAreSkippedAndNoGroupMeansNoPhotos() {
        assertEquals(List.of("u2"), OralScreeningInputs.from(screening(List.of(row(null, "No"), row("u2", "No")), null), TanuhConceptsFixture.defaults()).getPhotoUrls());
        OralScreeningInputs none = OralScreeningInputs.from(screening(null, null), TanuhConceptsFixture.defaults());
        assertFalse(none.hasPhotos());
    }

    @Test
    public void aSingleRowGroupStoredAsAMapIsReadAsOneRow() {
        assertEquals(List.of("u1"), OralScreeningInputs.from(screening(row("u1", "No"), null), TanuhConceptsFixture.defaults()).getPhotoUrls());
    }

    @Test
    public void aScreeningCapturedBeforeTheGroupsGivesItsPhotoSlots() {
        GeneralEncounter s = new GeneralEncounter();
        s.addObservation("Photo 3 (image)", "old3");
        s.addObservation("Photo 1 (image)", "old1");
        assertEquals(List.of("old1", "old3"), OralScreeningInputs.from(s, TanuhConceptsFixture.defaults()).getPhotoUrls());
    }

    @Test
    public void theGroupsWinOverTheOldSlots() {
        GeneralEncounter s = screening(List.of(row("t1", "No")), null);
        s.addObservation("Photo 1 (image)", "old1");
        assertEquals(List.of("t1"), OralScreeningInputs.from(s, TanuhConceptsFixture.defaults()).getPhotoUrls());
    }

    private static final String FINAL_VERDICT = "Egde AI Final Verdict";

    private static GeneralEncounter answered(Object finalVerdict) {
        GeneralEncounter s = screening(List.of(row("u1", "No")), null);
        s.addObservation(FINAL_VERDICT, finalVerdict);
        return s;
    }

    // The phone's model fills in the screening's read-only "Egde AI Final Verdict"; its answer is what Tanuh is told.
    @Test
    public void theEdgeVerdictIsTheAnswerToTheFinalVerdictQuestion() {
        assertEquals("Suspicious", OralScreeningInputs.from(answered("Suspicious"), TanuhConceptsFixture.defaults()).getEdgeVerdict());
        assertEquals("Non-Suspicious", OralScreeningInputs.from(answered("Non-Suspicious"), TanuhConceptsFixture.defaults()).getEdgeVerdict());
    }

    // The job works nothing out from the photos' own verdicts (Adam, 20 Nov 2026), so a screening saved before the
    // question was on the form has none.
    @Test
    public void aScreeningWithoutTheAnswerHasNoEdgeVerdictWhateverItsPhotosSay() {
        GeneralEncounter s = screening(List.of(row("u1", "No")), null);
        s.addObservation("Image-wise AI Assessment", List.of(Map.of("AI Verdict", "Suspicious")));
        assertNull(OralScreeningInputs.from(s, TanuhConceptsFixture.defaults()).getEdgeVerdict());
    }

    @Test
    public void anAnswerIsMatchedWithoutRegardToCaseAndMayComeAsAList() {
        assertEquals("Non-Suspicious", OralScreeningInputs.from(answered(List.of("non-suspicious")), TanuhConceptsFixture.defaults()).getEdgeVerdict());
    }

    @Test
    public void anAnswerThatIsNeitherVerdictIsNone() {
        assertNull(OralScreeningInputs.from(answered("Unclear"), TanuhConceptsFixture.defaults()).getEdgeVerdict());
        assertNull(OralScreeningInputs.from(answered(" "), TanuhConceptsFixture.defaults()).getEdgeVerdict());
    }

    @Test
    public void theVerdictIsReadThroughTheOrganisationsNamesButSentInTanuhsWords() {
        TanuhConcepts renamed = TanuhConceptsFixture.editedBy(rows -> {
            rows.replaceAll(r -> switch (r.key()) {
                case "edge_ai_final_verdict" -> new TanuhConcepts.Mapping(r.key(), "Edge AI Final Verdict");
                case "suspicious" -> new TanuhConcepts.Mapping(r.key(), "Positive");
                default -> r;
            });
            return rows;
        });
        GeneralEncounter s = screening(List.of(row("u1", "No")), null);
        s.addObservation("Edge AI Final Verdict", "Positive");
        assertEquals("Suspicious", OralScreeningInputs.from(s, renamed).getEdgeVerdict());
    }
}
