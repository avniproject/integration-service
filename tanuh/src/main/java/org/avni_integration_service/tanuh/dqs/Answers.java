package org.avni_integration_service.tanuh.dqs;

import org.avni_integration_service.avni.domain.AvniBaseContract;
import org.avni_integration_service.tanuh.domain.TanuhConcepts;
import org.avni_integration_service.tanuh.domain.TanuhConcepts.Concept;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Reads Avni observations as the data quality service's value types. Every reader answers null for a value that is
// absent or not of the shape expected, never throwing: an odd answer is a finding for the service, not a reason to
// hold the screening back.
final class Answers {
    private Answers() {
    }

    static void put(Map<String, Object> payload, String field, Object value) {
        if (value != null) payload.put(field, value);
    }

    // A coded or text answer as one string; the first of a multi-select.
    static String text(Object value) {
        List<String> all = strings(value);
        return all.isEmpty() ? null : all.get(0);
    }

    // A text answer, or a coded answer as a string or a list of them.
    static List<String> strings(Object value) {
        if (value instanceof String s) return s.isBlank() ? List.of() : List.of(s.trim());
        if (value instanceof List<?> list)
            return list.stream().filter(String.class::isInstance).map(String.class::cast).map(String::trim).filter(s -> !s.isBlank()).toList();
        return List.of();
    }

    static Boolean yesNo(Object value, TanuhConcepts concepts) {
        String answer = text(value);
        if (concepts.name(Concept.YES).equalsIgnoreCase(answer)) return true;
        if (concepts.name(Concept.NO).equalsIgnoreCase(answer)) return false;
        return null;
    }

    static String lower(Object value) {
        String answer = text(value);
        return answer == null ? null : answer.toLowerCase(Locale.ROOT);
    }

    static BigDecimal decimal(Object value) {
        if (value instanceof Boolean) return null;
        if (value instanceof Number n) return new BigDecimal(n.toString());
        if (value instanceof String s) {
            try {
                return new BigDecimal(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    // The rows of a repeatable question group; a single row comes as a map on its own.
    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> rows(Object group) {
        if (group instanceof List<?> list)
            return list.stream().filter(Map.class::isInstance).map(row -> (Map<String, Object>) row).toList();
        if (group instanceof Map<?, ?> single) return List.of((Map<String, Object>) single);
        return List.of();
    }

    static String audit(AvniBaseContract entity, String field) {
        if (!(entity.get("audit") instanceof Map<?, ?> audit)) return null;
        return text(audit.get(field));
    }

    // An ISO date-time as Avni writes it, with its zone, passed to the service unchanged; null for anything else.
    static String dateTime(Object value) {
        String text = text(value);
        if (text == null) return null;
        try {
            OffsetDateTime.parse(text);
            return text;
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // An ISO date as Avni writes a date-only field; null for anything else.
    static LocalDate date(Object value) {
        String text = text(value);
        if (text == null) return null;
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // A visit's answers read by the job's names for its questions.
    record Visit(AvniBaseContract entity, TanuhConcepts concepts) {
        Object get(Concept question) {
            return entity.getObservation(concepts.name(question));
        }

        String text(Concept question) {
            return Answers.text(get(question));
        }

        String lower(Concept question) {
            return Answers.lower(get(question));
        }

        Boolean yesNo(Concept question) {
            return Answers.yesNo(get(question), concepts);
        }
    }
}
