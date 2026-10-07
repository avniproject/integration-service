package org.avni_integration_service.avni.client;

import org.avni_integration_service.util.ObjectJsonMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class AvniHttpClientPatchTest {
    private final RestTemplate restTemplate = mock(RestTemplate.class);
    private final AvniHttpClient avniHttpClient = new AvniHttpClient();

    @AfterEach
    public void tearDown() {
        AvniHttpClient.removeAvniSession();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void patchSendsThePatchMethodAndKeepsNullValuesInTheBody() {
        ReflectionTestUtils.setField(avniHttpClient, "restTemplate", restTemplate);
        avniHttpClient.setAvniSession(new AvniSession("http://localhost:8021", "integration@tanuh_uat_local", "any", false));
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.PATCH), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{}"));
        Map<String, Object> observations = new HashMap<>();
        observations.put("High risk model status", "Scored");
        observations.put("High risk model result", null);

        avniHttpClient.patch("/api/encounter/abc", Map.of("observations", observations), String.class);

        ArgumentCaptor<URI> uri = ArgumentCaptor.forClass(URI.class);
        ArgumentCaptor<HttpEntity> entity = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(uri.capture(), eq(HttpMethod.PATCH), entity.capture(), eq(String.class));
        assertEquals("http://localhost:8021/api/encounter/abc", uri.getValue().toString());
        Map<String, Object> sent = ObjectJsonMapper.readValue((String) entity.getValue().getBody(), Map.class);
        assertEquals(Set.of("observations"), sent.keySet());
        Map<String, Object> sentObservations = (Map<String, Object>) sent.get("observations");
        assertEquals("Scored", sentObservations.get("High risk model status"));
        assertTrue(sentObservations.containsKey("High risk model result"), "a null value must be sent, to clear the observation");
        assertNull(sentObservations.get("High risk model result"));
        assertEquals("integration@tanuh_uat_local", entity.getValue().getHeaders().getFirst("user-name"));
    }
}
