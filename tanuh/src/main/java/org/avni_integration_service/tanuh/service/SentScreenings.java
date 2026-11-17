package org.avni_integration_service.tanuh.service;

import org.avni_integration_service.tanuh.domain.TanuhConcepts.Concept;

import java.util.List;
import java.util.Optional;

// What the job settled with Tanuh's high-risk model service for each screening of the organisation, kept outside Avni
// because a phone's edit replaces every answer on the screening, OCSHR Call Made included. A screening found here is
// never sent with other photos: an edit after the first send never reaches Tanuh's model.
public interface SentScreenings {
    record SentScreening(Optional<List<String>> imageUris, Optional<Concept> callMade) {
    }

    Optional<SentScreening> find(String screeningUuid);

    // The photos of the first send, kept before it is made, so a send whose answer is lost is repeated as it was.
    // A screening already found here keeps what it has.
    void attempt(String screeningUuid, List<String> imageUris);

    // The job's answer, YES once Tanuh's service took the screening, NO when it is not sent. Kept before it is written
    // on the screening, so the screening can always be put right from here.
    void answer(String screeningUuid, Concept callMade);
}
