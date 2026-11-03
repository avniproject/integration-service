package org.avni_integration_service.tanuh.dqs;

import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.Subject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Visits and patients shaped as the Avni API lists them for Tanuh.
final class Fixtures {
    static final String WORKER = "asha1@tanuh_uat";
    static final String PHYSICIAN = "doctor@tanuh_uat";
    static final String VISIT_AT = "2026-10-06T05:30:00.000Z";
    static final String CREATED_AT = "2026-10-06T05:41:00.000Z";
    static final String MODIFIED_AT = "2026-10-06T07:00:00.000Z";

    private Fixtures() {
    }

    static GeneralEncounter visit(String uuid, String type, String createdBy, String modifiedBy) {
        GeneralEncounter e = new GeneralEncounter();
        e.setUuid(uuid);
        e.setSubjectId("patient-1");
        e.setEncounterType(type);
        e.setEncounterDateTime(VISIT_AT);
        e.setVoided(false);
        e.set("Encounter Coordinates", Map.of("x", 13.0712, "y", 77.7981));
        Map<String, Object> audit = new HashMap<>();
        audit.put("Created at", CREATED_AT);
        audit.put("Created by", createdBy);
        audit.put("Last modified at", MODIFIED_AT);
        audit.put("Last modified by", modifiedBy);
        e.set("audit", audit);
        return e;
    }

    static GeneralEncounter screening(String uuid) {
        return visit(uuid, "Oral Screening", WORKER, WORKER);
    }

    static Subject patient() {
        Subject p = new Subject();
        p.setUuid("patient-1");
        p.setVoided(false);
        p.set("Location ID", "village-uuid-1");
        p.set("Location Properties", Map.of("LGD code", 625529));
        p.addObservation("Date of birth", "1980-10-07");
        return p;
    }

    // One photo row: the worker's answer, and the model's in the parallel group.
    static Map<String, Object> photoRow(String url, String suspiciousLesion) {
        Map<String, Object> row = new HashMap<>();
        row.put("Oral Image", url);
        if (suspiciousLesion != null) row.put("Suspicious Lesion?", suspiciousLesion);
        return row;
    }

    static Map<String, Object> aiRow(String verdict) {
        Map<String, Object> row = new HashMap<>();
        if (verdict != null) row.put("AI Verdict", verdict);
        return row;
    }

    static List<Map<String, Object>> rows(Map<String, Object>... rows) {
        return new ArrayList<>(List.of(rows));
    }

    static Map<String, Object> reviewRow(String url, String verdict, String acceptable, String highestRisk) {
        Map<String, Object> row = new HashMap<>();
        row.put("Oral Image", url);
        if (verdict != null) row.put("Physician verdict", verdict);
        if (acceptable != null) row.put("Acceptable Quality of the photo?", acceptable);
        if (highestRisk != null) row.put("Highest Risk Photo?", highestRisk);
        return row;
    }
}
