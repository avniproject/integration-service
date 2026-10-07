package org.avni_integration_service.avni.repository;

import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
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
}
