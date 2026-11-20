package org.avni_integration_service.tanuh.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.avni_integration_service.tanuh.domain.TanuhConcepts.Concept.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

// The bundled names, checked against the names Tanuh's configuration creates.
public class TanuhNamesTest {
    private final TanuhConcepts concepts = TanuhConceptsFixture.defaults();

    @Test
    public void theQualityVerdictIsWrittenToTheseConcepts() {
        assertEquals(List.of("QC status", "QC Pass", "QC Fail", "QC check time"),
                List.of(concepts.name(QC_STATUS), concepts.name(QC_PASS), concepts.name(QC_FAIL), concepts.name(QC_CHECK_TIME)));
        assertEquals("OCSHR Call Made", concepts.name(OCSHR_CALL_MADE));
    }

    @Test
    public void theFormsQuestionsAreNamedAsOnTanuhsForms() {
        assertEquals(List.of("Oral Screening", "Take photos of all lesions and 1 photo without lesion", "ORAL SCREENING",
                        "Oral Image", "Suspicious Lesion?", "Yes"),
                List.of(concepts.name(ORAL_SCREENING), concepts.name(TAKE_PHOTOS), concepts.name(GUIDED_PHOTOS),
                        concepts.name(ORAL_IMAGE), concepts.name(SUSPICIOUS_LESION), concepts.name(YES)));
        assertEquals("Photo 3 — AI verdict", concepts.name(LEGACY_AI_VERDICT, 3));
        // Spelt "Egde" on Tanuh UAT's Oral Screening form (bundle of 20 Nov 2026); the job must ask for it as spelt.
        assertEquals("Egde AI Final Verdict", concepts.name(EDGE_AI_FINAL_VERDICT));
    }
}
