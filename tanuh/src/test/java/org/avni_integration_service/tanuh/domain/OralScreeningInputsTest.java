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
}
