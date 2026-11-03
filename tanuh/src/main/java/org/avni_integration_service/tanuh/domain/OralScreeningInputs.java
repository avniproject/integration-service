package org.avni_integration_service.tanuh.domain;

import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.tanuh.domain.TanuhConcepts.Concept;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class OralScreeningInputs {
    private final List<String> photoUrls;

    private OralScreeningInputs(List<String> photoUrls) {
        this.photoUrls = photoUrls;
    }

    // The photos in the rows of "Take photos of all lesions and 1 photo without lesion" when it has any, otherwise in
    // those of "ORAL SCREENING".
    public static OralScreeningInputs from(GeneralEncounter screening, TanuhConcepts concepts) {
        List<Map<String, Object>> rows = rows(screening.getObservation(concepts.name(Concept.TAKE_PHOTOS)));
        if (rows.isEmpty()) rows = rows(screening.getObservation(concepts.name(Concept.GUIDED_PHOTOS)));
        List<String> urls = new ArrayList<>();
        for (Map<String, Object> row : rows) urls.addAll(strings(row.get(concepts.name(Concept.ORAL_IMAGE))));
        return new OralScreeningInputs(List.copyOf(urls));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> rows(Object group) {
        if (group instanceof List<?> list) {
            return list.stream().filter(Map.class::isInstance).map(row -> (Map<String, Object>) row).toList();
        }
        if (group instanceof Map<?, ?> single) return List.of((Map<String, Object>) single);
        return List.of();
    }

    // A photo URL comes as a string or as a list of them.
    private static List<String> strings(Object value) {
        if (value instanceof String s) return s.isBlank() ? List.of() : List.of(s);
        if (value instanceof List<?> list) {
            return list.stream().filter(String.class::isInstance).map(String.class::cast).filter(s -> !s.isBlank()).toList();
        }
        return List.of();
    }

    public List<String> getPhotoUrls() {
        return photoUrls;
    }

    public boolean hasPhotos() {
        return !photoUrls.isEmpty();
    }
}
