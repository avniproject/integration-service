package org.avni_integration_service.tanuh.worker;

import org.avni_integration_service.tanuh.domain.TanuhConcepts.Concept;
import org.avni_integration_service.tanuh.service.SentScreenings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// The job's record of sends, in memory, as DbSentScreenings keeps it in the integration service's database.
public class FakeSentScreenings implements SentScreenings {
    public final Map<String, SentScreening> rows = new HashMap<>();

    @Override
    public Optional<SentScreening> find(String screeningUuid) {
        return Optional.ofNullable(rows.get(screeningUuid));
    }

    @Override
    public void attempt(String screeningUuid, List<String> imageUris) {
        rows.putIfAbsent(screeningUuid, new SentScreening(Optional.of(List.copyOf(imageUris)), Optional.empty()));
    }

    @Override
    public void answer(String screeningUuid, Concept callMade) {
        SentScreening row = rows.get(screeningUuid);
        rows.put(screeningUuid, new SentScreening(row == null ? Optional.empty() : row.imageUris(), Optional.of(callMade)));
    }
}
