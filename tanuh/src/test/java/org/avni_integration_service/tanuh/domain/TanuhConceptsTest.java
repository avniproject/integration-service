package org.avni_integration_service.tanuh.domain;

import org.avni_integration_service.tanuh.domain.TanuhConcepts.Mapping;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.avni_integration_service.tanuh.domain.TanuhConcepts.Concept.*;
import static org.junit.jupiter.api.Assertions.*;

public class TanuhConceptsTest {
    @Test
    public void theBundleMapsEveryConceptTheJobUses() {
        TanuhConcepts concepts = TanuhConceptsFixture.defaults();
        for (TanuhConcepts.Concept concept : TanuhConcepts.Concept.values())
            assertNotNull(concepts.name(concept), concept.key());
        assertEquals(TanuhConcepts.Concept.values().length, TanuhConceptsFixture.bundleRows().size());
    }

    @Test
    public void anEditedRowIsTheNameRead() {
        TanuhConcepts concepts = TanuhConceptsFixture.editedBy(rows -> replace(rows, "qc_pass", "DQ Pass"));
        assertEquals("DQ Pass", concepts.name(QC_PASS));
        assertEquals("QC Fail", concepts.name(QC_FAIL));
        assertEquals("QC Pass", TanuhConceptsFixture.defaults().name(QC_PASS));
    }

    @Test
    public void rowsTheJobCannotTrustAreRefusedWithEveryFaultNamed() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> TanuhConceptsFixture.editedBy(rows -> {
            rows.removeIf(row -> row.key().equals("qc_status") || row.key().equals("areca_nut"));
            rows.add(new Mapping("qc_statsu", "QC status"));
            rows.add(new Mapping("oral_screening", "Oral Screening"));
            replace(rows, "legacy_photo", "Photo image");
            replace(rows, "oral_image", "Photo %d");
            rows.add(new Mapping("alcohol", " "));
            return rows;
        }));
        String message = e.getMessage();
        for (String fault : List.of(
                "'qc_statsu' is not a key the job uses",
                "'oral_screening' is given twice",
                "'legacy_photo' must hold the photo number as %d",
                "'oral_image' must not hold the photo number as %d",
                "'alcohol' has no Avni name",
                "no row for areca_nut, qc_status"))
            assertTrue(message.contains(fault), fault + " not in: " + message);
        assertTrue(message.startsWith("Tanuh mapping: "), message);
    }

    @Test
    public void noRowsAtAllIsRefused() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> TanuhConcepts.from(List.of()));
        assertTrue(e.getMessage().contains("no row for yes, no, suspicious, non_suspicious, non_suspicious_legacy, oral_screening"), e.getMessage());
    }

    // A legacy name is formatted with each photo's number at runtime, so a name that would not format is refused here.
    @Test
    public void aLegacyNameThatWouldNotFormatIsRefusedAtLoad() {
        for (String name : List.of("Photo %d (100%)", "Photo %d of %d", "Photo %s")) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> TanuhConceptsFixture.editedBy(rows -> replace(rows, "legacy_photo", name)), name);
            assertTrue(e.getMessage().contains("'legacy_photo' must hold the photo number as"), name + ": " + e.getMessage());
        }
    }

    @Test
    public void aLegacyNameWithALiteralPercentFormats() {
        TanuhConcepts concepts = TanuhConceptsFixture.editedBy(rows -> replace(rows, "legacy_photo", "Photo %d (100%%)"));
        assertEquals("Photo 3 (100%)", concepts.name(TanuhConcepts.Concept.LEGACY_PHOTO, 3));
    }

    static List<Mapping> replace(List<Mapping> rows, String key, String name) {
        List<Mapping> result = new ArrayList<>();
        for (Mapping row : rows) result.add(row.key().equals(key) ? new Mapping(key, name) : row);
        rows.clear();
        rows.addAll(result);
        return rows;
    }
}
