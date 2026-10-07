package org.avni_integration_service.avni.repository;

import org.avni_integration_service.avni.client.AvniHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AvniMediaRepositoryTest {
    @Mock
    private AvniHttpClient avniHttpClient;

    @Test
    public void getSignedDownloadUrlAsksTheServerToSignTheStoredUrl() {
        String stored = "https://s3.ap-south-1.amazonaws.com/prod-user-media/tanuh/photo.jpg";
        String signed = "https://prod-user-media.s3.amazonaws.com/tanuh/photo.jpg?X-Amz-Signature=abc";
        when(avniHttpClient.get("/media/signedUrl", Map.of("url", stored), String.class)).thenReturn(ResponseEntity.ok(signed));

        assertEquals(signed, new AvniMediaRepository(avniHttpClient).getSignedDownloadUrl(stored));
    }
}
