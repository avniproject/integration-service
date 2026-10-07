package org.avni_integration_service.avni.repository;

import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.GeneralEncountersResponse;
import org.avni_integration_service.util.FormatAndParseUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

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
        Date since = new Date(0);

        repository.getGeneralEncounters(since, "Oral Screening", 1000);

        ArgumentCaptor<Map<String, String>> params = ArgumentCaptor.forClass(Map.class);
        verify(avniHttpClient).get(eq("/api/encounters"), params.capture(), eq(GeneralEncountersResponse.class));
        assertEquals(Map.of(
                "encounterType", "Oral Screening",
                "lastModifiedDateTime", FormatAndParseUtil.toISODateTimeString(since),
                "size", "1000"), params.getValue());
    }

    @Test
    public void getGeneralEncountersRefusesAPageSizeOutsideOneToAThousand() {
        assertThrows(IllegalArgumentException.class, () -> repository.getGeneralEncounters(new Date(), "Oral Screening", 1001));
        assertThrows(IllegalArgumentException.class, () -> repository.getGeneralEncounters(new Date(), "Oral Screening", 0));
    }
}
