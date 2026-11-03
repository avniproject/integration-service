package org.avni_integration_service.tanuh.dqs;

import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.tanuh.domain.TanuhConcepts;
import org.avni_integration_service.tanuh.domain.TanuhConcepts.Concept;
import org.avni_integration_service.tanuh.dqs.ImageVerdicts.Image;
import org.avni_integration_service.tanuh.dqs.ImageVerdicts.Verdict;

import java.util.List;
import java.util.Map;

// An Oral Screening as the service's oral_screening record: the worker who filled it, the habit answers, the
// examination answers, and the photo counts the agreement rules read.
public final class OralScreeningRecord {
    private OralScreeningRecord() {
    }

    public static Map<String, Object> from(GeneralEncounter screening, Subject patient, TanuhConcepts concepts) {
        Answers.Visit visit = new Answers.Visit(screening, concepts);
        Map<String, Object> payload = Envelope.of(screening, patient, concepts, Answers.audit(screening, Envelope.CREATED_BY),
                Answers.audit(screening, Envelope.CREATED_AT));
        // The under-30 question is asked only of them: "No" to proceeding means the screening was not done.
        Boolean proceed = visit.yesNo(Concept.SCREENING_NOT_REQUIRED_PROCEED);
        Answers.put(payload, "screening_not_required", proceed == null ? null : !proceed);
        Answers.put(payload, "any_symptoms", visit.yesNo(Concept.ANY_SYMPTOMS));
        Answers.put(payload, "able_to_open_mouth", visit.yesNo(Concept.ABLE_TO_OPEN_MOUTH));
        Answers.put(payload, "flw_sees_lesion", visit.yesNo(Concept.SEE_ANY_LESIONS));
        Answers.put(payload, "flw_overrides_ai", visit.yesNo(Concept.WORKER_OVERRIDES_AI));
        Answers.put(payload, "precapture_ack", visit.yesNo(Concept.PRECAPTURE_ACK));
        Answers.put(payload, "smokeless_tobacco", visit.lower(Concept.SMOKELESS_TOBACCO));
        Answers.put(payload, "bidi_cigarette", visit.lower(Concept.BIDI_CIGARETTE));
        Answers.put(payload, "areca_nut", visit.lower(Concept.ARECA_NUT));
        Answers.put(payload, "alcohol", visit.lower(Concept.ALCOHOL));
        Answers.put(payload, "alcohol_frequency", visit.text(Concept.ALCOHOL_FREQUENCY));
        Answers.put(payload, "place_of_referral_id", placeOfReferral(visit.get(Concept.PLACE_OF_REFERRAL)));

        List<Image> images = new ImageVerdicts(concepts).ofScreening(screening);
        List<Verdict> worker = images.stream().map(Image::worker).toList();
        List<Verdict> ai = images.stream().map(Image::ai).toList();
        payload.put("image_count", images.size());
        payload.put("images_ai_missing", ImageVerdicts.missing(ai));
        payload.put("images_rater_missing", ImageVerdicts.missing(worker));
        ImageVerdicts.putAgreement(payload, worker, ai, "images_rater_and_ai_suspicious", "images_rater_only_suspicious",
                "images_ai_only_suspicious", "images_neither_suspicious");
        return payload;
    }

    // A Location answer comes back as the facility's address levels by type, most specific first, or as the bare
    // uuid when the server could not resolve it. The service only asks whether a place is named.
    private static String placeOfReferral(Object value) {
        if (value instanceof Map<?, ?> levels) {
            for (Map.Entry<?, ?> level : levels.entrySet()) {
                String name = Answers.text(level.getValue());
                if (name != null) return name;
            }
            return null;
        }
        return Answers.text(value);
    }
}
