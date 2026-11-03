package org.avni_integration_service.tanuh.dqs;

import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.tanuh.domain.TanuhConcepts;
import org.avni_integration_service.tanuh.domain.TanuhConcepts.Concept;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

// The fields every record type shares, read from the visit and its patient.
final class Envelope {
    static final String ENCOUNTER_DATE_TIME = "Encounter date time";
    static final String ENCOUNTER_COORDINATES = "Encounter Coordinates";
    static final String LOCATION_PROPERTIES = "Location Properties";
    static final String CREATED_AT = "Created at";
    static final String CREATED_BY = "Created by";
    static final String LAST_MODIFIED_AT = "Last modified at";
    static final String LAST_MODIFIED_BY = "Last modified by";

    private Envelope() {
    }

    // visit_at is the visit as the device recorded it; received_at is when this version reached Avni, which the
    // service reads as the sync delay. village_id is the LGD code of the patient's village. GPS is the visit's fix, x
    // being latitude as the app writes it.
    static Map<String, Object> of(GeneralEncounter encounter, Subject patient, TanuhConcepts concepts, String workerId,
                                  String recordedAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("record_id", encounter.getUuid());
        payload.put("beneficiary_id", encounter.getSubjectId());
        Answers.put(payload, "worker_id", workerId);
        Answers.put(payload, "village_id", villageId(patient, concepts));
        Answers.put(payload, "visit_at", Answers.dateTime(encounter.get(ENCOUNTER_DATE_TIME)));
        Answers.put(payload, "recorded_at", Answers.dateTime(recordedAt));
        Answers.put(payload, "received_at", Answers.dateTime(Answers.audit(encounter, LAST_MODIFIED_AT)));
        if (encounter.get(ENCOUNTER_COORDINATES) instanceof Map<?, ?> point) {
            Answers.put(payload, "gps_lat", Answers.decimal(point.get("x")));
            Answers.put(payload, "gps_lon", Answers.decimal(point.get("y")));
        }
        return payload;
    }

    // The LGD code the patient's village carries as a location attribute, which /api/subject lists under the
    // village's properties. The service finds the village, its taluk and its district by it. Null when the village has
    // none: the service then has no village for the record.
    static String villageId(Subject patient, TanuhConcepts concepts) {
        if (!(patient.get(LOCATION_PROPERTIES) instanceof Map<?, ?> properties)) return null;
        Object code = properties.get(concepts.name(Concept.LGD_CODE));
        BigDecimal number = Answers.decimal(code);
        if (number == null) return null;
        try {
            return number.toBigIntegerExact().toString();
        } catch (ArithmeticException e) {
            return null;
        }
    }
}
