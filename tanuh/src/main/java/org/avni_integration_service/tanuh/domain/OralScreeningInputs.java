package org.avni_integration_service.tanuh.domain;

import org.avni_integration_service.avni.domain.GeneralEncounter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class OralScreeningInputs {
    private final List<String> photoUrls;
    private final WorkerOpinion workerOpinion;

    private OralScreeningInputs(List<String> photoUrls, WorkerOpinion workerOpinion) {
        this.photoUrls = photoUrls;
        this.workerOpinion = workerOpinion;
    }

    // The rows of "Take photos of all lesions and 1 photo without lesion" when it has any, otherwise those of
    // "ORAL SCREENING". The worker's opinion is "Suspicious Lesion?" on those rows only; a referral answer does not count.
    public static OralScreeningInputs from(GeneralEncounter screening) {
        List<Map<String, Object>> rows = rows(screening.getObservation(TanuhConcepts.TAKE_PHOTOS));
        if (rows.isEmpty()) rows = rows(screening.getObservation(TanuhConcepts.GUIDED_PHOTOS));
        List<String> urls = new ArrayList<>();
        boolean suspicious = false;
        for (Map<String, Object> row : rows) {
            urls.addAll(strings(row.get(TanuhConcepts.ORAL_IMAGE)));
            suspicious |= strings(row.get(TanuhConcepts.SUSPICIOUS_LESION)).contains(TanuhConcepts.YES);
        }
        return new OralScreeningInputs(List.copyOf(urls), suspicious ? WorkerOpinion.SUSPICIOUS : WorkerOpinion.NOT_SUSPICIOUS);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> rows(Object group) {
        if (group instanceof List<?> list) {
            return list.stream().filter(Map.class::isInstance).map(row -> (Map<String, Object>) row).toList();
        }
        if (group instanceof Map<?, ?> single) return List.of((Map<String, Object>) single);
        return List.of();
    }

    // A photo URL or a coded answer comes as a string or as a list of them.
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

    public WorkerOpinion getWorkerOpinion() {
        return workerOpinion;
    }

    public boolean hasPhotos() {
        return !photoUrls.isEmpty();
    }
}
