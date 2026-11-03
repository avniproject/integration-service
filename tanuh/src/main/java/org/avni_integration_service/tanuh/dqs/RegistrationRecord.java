package org.avni_integration_service.tanuh.dqs;

import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.tanuh.domain.TanuhConcepts;
import org.avni_integration_service.tanuh.domain.TanuhConcepts.Concept;

import java.time.LocalDate;
import java.time.Period;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

// A patient as the service's registration record, read from /api/subject and sent ahead of one of their screenings.
// The rules that compare a visit with the person, such as a visit dated before the registration, look up the
// registration's values for that beneficiary.
public final class RegistrationRecord {
    static final String REGISTRATION_COORDINATES = "Registration Coordinates";
    static final String MIDDLE_NAME = "Middle name";
    private static final List<String> SEXES = List.of("male", "female", "other");

    private RegistrationRecord() {
    }

    // The record is the patient as they stood at the screening it is sent with, so its times are the screening's:
    // visit_at its visit, recorded_at its entry and received_at when it reached Avni. registered_on is the
    // registration's own day, which the rules that compare a visit with the registration read.
    public static Map<String, Object> from(GeneralEncounter screening, Subject patient, TanuhConcepts concepts) {
        Map<String, Object> payload = new LinkedHashMap<>();
        LocalDate registeredOn = Answers.date(patient.get(Subject.RegistrationDateFieldName));
        LocalDate dateOfBirth = Answers.date(patient.getObservation(Subject.DateOfBirth));
        payload.put("record_id", patient.getUuid());
        payload.put("beneficiary_id", patient.getUuid());
        Answers.put(payload, "worker_id", Answers.audit(patient, Envelope.CREATED_BY));
        Answers.put(payload, "village_id", Envelope.villageId(patient, concepts));
        Answers.put(payload, "visit_at", Answers.dateTime(screening.get(Envelope.ENCOUNTER_DATE_TIME)));
        Answers.put(payload, "recorded_at", Answers.dateTime(Answers.audit(screening, Envelope.CREATED_AT)));
        Answers.put(payload, "received_at", Answers.dateTime(Answers.audit(screening, Envelope.LAST_MODIFIED_AT)));
        if (patient.get(REGISTRATION_COORDINATES) instanceof Map<?, ?> point) {
            Answers.put(payload, "gps_lat", Answers.decimal(point.get("x")));
            Answers.put(payload, "gps_lon", Answers.decimal(point.get("y")));
        }
        Answers.put(payload, "registered_on", registeredOn == null ? null : registeredOn.toString());
        Answers.put(payload, "age", dateOfBirth == null || registeredOn == null || dateOfBirth.isAfter(registeredOn) ? null
                : Period.between(dateOfBirth, registeredOn).getYears());
        Answers.put(payload, "sex", sex(Answers.lower(patient.getObservation(Subject.GenderName))));
        Answers.put(payload, "date_of_birth", dateOfBirth == null ? null : dateOfBirth.toString());
        // The service hashes the name and drops the address on arrival, keeping only whether one was given.
        Answers.put(payload, "full_name", fullName(patient));
        Answers.put(payload, "full_address", Answers.text(patient.getObservation(concepts.name(Concept.FULL_ADDRESS))));
        return payload;
    }

    private static String sex(String gender) {
        return gender != null && SEXES.contains(gender) ? gender : null;
    }

    private static String fullName(Subject patient) {
        List<String> parts = Stream.of(patient.getFirstName(), Answers.text(patient.getObservation(MIDDLE_NAME)), patient.getLastName())
                .map(Answers::text).filter(part -> part != null).toList();
        return parts.isEmpty() ? null : String.join(" ", parts);
    }
}
