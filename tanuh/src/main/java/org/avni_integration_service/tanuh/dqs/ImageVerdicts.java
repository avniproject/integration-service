package org.avni_integration_service.tanuh.dqs;

import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.tanuh.domain.TanuhConcepts;
import org.avni_integration_service.tanuh.domain.TanuhConcepts.Concept;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// The verdicts on a screening's photographs, one row per photo, as the health worker and the on-device model gave
// them, and the 2x2 counts the service's agreement rules read from them.
final class ImageVerdicts {
    enum Verdict {SUSPICIOUS, NOT_SUSPICIOUS, MISSING}

    // url is the photo as stored, without a query string.
    record Image(String url, Verdict worker, Verdict ai) {
    }

    private final TanuhConcepts concepts;

    ImageVerdicts(TanuhConcepts concepts) {
        this.concepts = concepts;
    }

    private String name(Concept concept) {
        return concepts.name(concept);
    }

    private String name(Concept concept, int slot) {
        return concepts.name(concept, slot);
    }

    // The photos are in "Take photos of all lesions and 1 photo without lesion" when it has any, else in "ORAL
    // SCREENING"; the model's verdicts sit in "Image-wise AI Assessment", row i for photo i, with the legacy
    // per-row "AI verdict" as a fallback. Screenings captured before the groups hold "Photo N (image)" slots.
    List<Image> ofScreening(GeneralEncounter screening) {
        List<Map<String, Object>> rows = Answers.rows(screening.getObservation(name(Concept.TAKE_PHOTOS)));
        if (rows.isEmpty()) rows = Answers.rows(screening.getObservation(name(Concept.GUIDED_PHOTOS)));
        if (rows.isEmpty()) return legacySlots(screening);
        List<Map<String, Object>> aiRows = Answers.rows(screening.getObservation(name(Concept.AI_VERDICT_GROUP)));
        List<Image> images = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            String url = key(Answers.text(row.get(name(Concept.ORAL_IMAGE))));
            if (url == null) continue;
            Verdict worker = yesNoVerdict(row.get(name(Concept.SUSPICIOUS_LESION)));
            if (worker == Verdict.MISSING) worker = verdict(row.get(name(Concept.HEALTH_WORKER_VERDICT)));
            Verdict ai = i < aiRows.size() ? verdict(aiRows.get(i).get(name(Concept.AI_VERDICT))) : Verdict.MISSING;
            if (ai == Verdict.MISSING) ai = verdict(row.get(name(Concept.AI_VERDICT_LEGACY)));
            images.add(new Image(url, worker, ai));
        }
        return images;
    }

    private List<Image> legacySlots(GeneralEncounter screening) {
        List<Image> images = new ArrayList<>();
        for (int slot = 1; slot <= TanuhConcepts.LEGACY_PHOTO_SLOTS; slot++) {
            String url = key(Answers.text(screening.getObservation(name(Concept.LEGACY_PHOTO, slot))));
            if (url == null) continue;
            Verdict worker = verdict(screening.getObservation(name(Concept.LEGACY_HEALTH_WORKER_VERDICT, slot)));
            if (worker == Verdict.MISSING) worker = verdict(screening.getObservation(name(Concept.LEGACY_ASHA_VERDICT, slot)));
            Verdict ai = verdict(screening.getObservation(name(Concept.LEGACY_AI_VERDICT, slot)));
            images.add(new Image(url, worker, ai));
        }
        return images;
    }

    // The service's four agreement counts, over the photos both raters judged, plus how many each left unjudged.
    static void putAgreement(Map<String, Object> payload, List<Verdict> rater, List<Verdict> other,
                             String both, String raterOnly, String otherOnly, String neither) {
        int bothCount = 0, raterOnlyCount = 0, otherOnlyCount = 0, neitherCount = 0;
        for (int i = 0; i < rater.size(); i++) {
            Verdict r = rater.get(i), o = other.get(i);
            if (r == Verdict.MISSING || o == Verdict.MISSING) continue;
            if (r == Verdict.SUSPICIOUS && o == Verdict.SUSPICIOUS) bothCount++;
            else if (r == Verdict.SUSPICIOUS) raterOnlyCount++;
            else if (o == Verdict.SUSPICIOUS) otherOnlyCount++;
            else neitherCount++;
        }
        payload.put(both, bothCount);
        payload.put(raterOnly, raterOnlyCount);
        payload.put(otherOnly, otherOnlyCount);
        payload.put(neither, neitherCount);
    }

    static int missing(List<Verdict> verdicts) {
        return (int) verdicts.stream().filter(v -> v == Verdict.MISSING).count();
    }

    // The forms answer "Suspicious" or "Non-Suspicious"; older forms wrote "Non Suspicious". Any other answer is missing.
    Verdict verdict(Object value) {
        String answer = Answers.text(value);
        if (answer == null) return Verdict.MISSING;
        if (answer.equalsIgnoreCase(name(Concept.SUSPICIOUS))) return Verdict.SUSPICIOUS;
        if (answer.equalsIgnoreCase(name(Concept.NON_SUSPICIOUS)) || answer.equalsIgnoreCase(name(Concept.NON_SUSPICIOUS_LEGACY)))
            return Verdict.NOT_SUSPICIOUS;
        return Verdict.MISSING;
    }

    private Verdict yesNoVerdict(Object value) {
        Boolean yes = Answers.yesNo(value, concepts);
        return yes == null ? Verdict.MISSING : yes ? Verdict.SUSPICIOUS : Verdict.NOT_SUSPICIOUS;
    }

    static String key(String url) {
        if (url == null) return null;
        int query = url.indexOf('?');
        return query < 0 ? url : url.substring(0, query);
    }
}
