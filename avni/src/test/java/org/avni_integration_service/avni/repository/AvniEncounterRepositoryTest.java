package org.avni_integration_service.avni.repository;

import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.GeneralEncountersResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AvniEncounterRepositoryTest {
    @Mock
    private AvniHttpClient avniHttpClient;
    @InjectMocks
    private AvniEncounterRepository repository;

    @Test
    public void patchSendsOnlyTheObservationsToThatEncounter() {
        GeneralEncounter updated = new GeneralEncounter();
        when(avniHttpClient.patch(eq("/api/encounter/abc"), any(), eq(GeneralEncounter.class))).thenReturn(ResponseEntity.ok(updated));
        Map<String, Object> observations = new HashMap<>();
        observations.put("High risk model version", null);

        GeneralEncounter result = repository.patch("abc", observations);

        ArgumentCaptor<Object> body = ArgumentCaptor.forClass(Object.class);
        verify(avniHttpClient).patch(eq("/api/encounter/abc"), body.capture(), eq(GeneralEncounter.class));
        assertEquals(Map.of("observations", observations), body.getValue());
        assertSame(updated, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getGeneralEncountersWithAPageSizeSendsTypeCursorAndSizeButNoNow() {
        when(avniHttpClient.get(eq("/api/encounters"), anyMap(), eq(GeneralEncountersResponse.class)))
                .thenReturn(ResponseEntity.ok(new GeneralEncountersResponse()));

        // A cursor seeded in UTC wall-clock and read back from integrating_entity_status (#132).
        repository.getGeneralEncounters(Timestamp.valueOf("2026-10-15 00:00:00"), "Oral Screening", 1000);

        ArgumentCaptor<Map<String, String>> params = ArgumentCaptor.forClass(Map.class);
        verify(avniHttpClient).get(eq("/api/encounters"), params.capture(), eq(GeneralEncountersResponse.class));
        assertEquals(Map.of(
                "encounterType", "Oral Screening",
                "lastModifiedDateTime", "2026-10-15T00:00:00.000Z",
                "size", "1000"), params.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getGeneralEncountersSendsBackAnEncountersLastModifiedTimeExactlyAsTheServerGaveIt() {
        when(avniHttpClient.get(eq("/api/encounters"), anyMap(), eq(GeneralEncountersResponse.class)))
                .thenReturn(ResponseEntity.ok(new GeneralEncountersResponse()));
        GeneralEncounter screening = new GeneralEncounter();
        screening.set("audit", Map.of("Last modified at", "2026-10-07T08:45:12.345Z"));

        // #131 saves the cursor as the last screening's getLastModifiedDate().
        repository.getGeneralEncounters(screening.getLastModifiedDate(), "Oral Screening", 100);

        ArgumentCaptor<Map<String, String>> params = ArgumentCaptor.forClass(Map.class);
        verify(avniHttpClient).get(eq("/api/encounters"), params.capture(), eq(GeneralEncountersResponse.class));
        assertEquals("2026-10-07T08:45:12.345Z", params.getValue().get("lastModifiedDateTime"));
    }

    @Test
    public void getGeneralEncountersRefusesAPageSizeOutsideOneToAThousand() {
        assertThrows(IllegalArgumentException.class, () -> repository.getGeneralEncounters(new Date(), "Oral Screening", 1001));
        assertThrows(IllegalArgumentException.class, () -> repository.getGeneralEncounters(new Date(), "Oral Screening", 0));
    }

    // #131 reads one millisecond on its own, by offset, when a full page cannot move past it.
    @Test
    @SuppressWarnings("unchecked")
    public void getGeneralEncountersInAWindowSendsBothTimesThePageAndItsSize() {
        when(avniHttpClient.get(eq("/api/encounters"), anyMap(), eq(GeneralEncountersResponse.class)))
                .thenReturn(ResponseEntity.ok(new GeneralEncountersResponse()));
        GeneralEncounter screening = new GeneralEncounter();
        screening.set("audit", Map.of("Last modified at", "2026-10-07T08:45:12.345Z"));
        Date at = screening.getLastModifiedDate();

        repository.getGeneralEncounters(new Date(at.getTime() - 1), new Date(at.getTime() + 1), "Oral Screening", 1000, 2);

        ArgumentCaptor<Map<String, String>> params = ArgumentCaptor.forClass(Map.class);
        verify(avniHttpClient).get(eq("/api/encounters"), params.capture(), eq(GeneralEncountersResponse.class));
        assertEquals(Map.of(
                "encounterType", "Oral Screening",
                "lastModifiedDateTime", "2026-10-07T08:45:12.344Z",
                "now", "2026-10-07T08:45:12.346Z",
                "size", "1000",
                "page", "2"), params.getValue());
    }

    @Test
    public void getGeneralEncountersInAWindowRefusesABadPageSizeOrNumber() {
        assertThrows(IllegalArgumentException.class, () -> repository.getGeneralEncounters(new Date(0), new Date(), "Oral Screening", 1001, 0));
        assertThrows(IllegalArgumentException.class, () -> repository.getGeneralEncounters(new Date(0), new Date(), "Oral Screening", 0, 0));
        assertThrows(IllegalArgumentException.class, () -> repository.getGeneralEncounters(new Date(0), new Date(), "Oral Screening", 10, -1));
    }
}
