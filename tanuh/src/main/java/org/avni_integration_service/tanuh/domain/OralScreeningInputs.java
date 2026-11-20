package org.avni_integration_service.tanuh.domain;

import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.tanuh.domain.TanuhConcepts.Concept;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class OralScreeningInputs {
    // What Tanuh's model service is told the phone's model said, whatever the organisation calls the answers.
    private static final String SUSPICIOUS = "Suspicious";
    private static final String NON_SUSPICIOUS = "Non-Suspicious";

    private final List<String> photoUrls;
    private final String edgeVerdict;

    private OralScreeningInputs(List<String> photoUrls, String edgeVerdict) {
        this.photoUrls = photoUrls;
        this.edgeVerdict = edgeVerdict;
    }

    // The photos in the rows of "Take photos of all lesions and 1 photo without lesion" when it has any, otherwise in
    // those of "ORAL SCREENING", otherwise in the "Photo N (image)" slots of screenings captured before the groups.
    public static OralScreeningInputs from(GeneralEncounter screening, TanuhConcepts concepts) {
        List<Map<String, Object>> rows = rows(screening.getObservation(concepts.name(Concept.TAKE_PHOTOS)));
        if (rows.isEmpty()) rows = rows(screening.getObservation(concepts.name(Concept.GUIDED_PHOTOS)));
        List<String> urls = new ArrayList<>();
        if (rows.isEmpty()) {
            for (int slot = 1; slot <= TanuhConcepts.LEGACY_PHOTO_SLOTS; slot++) {
                urls.addAll(strings(screening.getObservation(concepts.name(Concept.LEGACY_PHOTO, slot))));
            }
        }
        for (Map<String, Object> row : rows) urls.addAll(strings(row.get(concepts.name(Concept.ORAL_IMAGE))));
        return new OralScreeningInputs(List.copyOf(urls), edgeVerdict(screening.getObservation(concepts.name(Concept.EDGE_AI_FINAL_VERDICT)), concepts));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> rows(Object group) {
        if (group instanceof List<?> list) {
            return list.stream().filter(Map.class::isInstance).map(row -> (Map<String, Object>) row).toList();
        }
        if (group instanceof Map<?, ?> single) return List.of((Map<String, Object>) single);
        return List.of();
    }

    // A photo URL, or a coded answer, comes as a string or as a list of them.
    private static List<String> strings(Object value) {
        if (value instanceof String s) return s.isBlank() ? List.of() : List.of(s);
        if (value instanceof List<?> list) {
            return list.stream().filter(String.class::isInstance).map(String.class::cast).filter(s -> !s.isBlank()).toList();
        }
        return List.of();
    }

    // The phone's model fills in the read-only "Egde AI Final Verdict" when the screening is saved: Suspicious when it
    // found any photo suspicious, otherwise Non-Suspicious. The job passes the answer on and works nothing out from the
    // photos' own verdicts (Adam, 20 Nov 2026), so any other answer, or none, as on a screening saved before the question
    // was on the form, is no verdict.
    private static String edgeVerdict(Object value, TanuhConcepts concepts) {
        List<String> answers = strings(value);
        if (answers.isEmpty()) return null;
        String answer = answers.get(0).trim();
        if (answer.equalsIgnoreCase(concepts.name(Concept.SUSPICIOUS))) return SUSPICIOUS;
        if (answer.equalsIgnoreCase(concepts.name(Concept.NON_SUSPICIOUS))) return NON_SUSPICIOUS;
        return null;
    }

    public List<String> getPhotoUrls() {
        return photoUrls;
    }

    // What the phone's model said of the screening, "Suspicious" or "Non-Suspicious", or null when the screening does not say.
    public String getEdgeVerdict() {
        return edgeVerdict;
    }

    public boolean hasPhotos() {
        return !photoUrls.isEmpty();
    }
}
