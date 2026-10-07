package org.avni_integration_service.avni.repository;

import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.avni.domain.Subject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AvniSubjectRepositoryTest {
    @Mock
    private AvniHttpClient avniHttpClient;
    @InjectMocks
    private AvniSubjectRepository repository;

    @Test
    public void getSubjectOrThrowReturnsADeletedPatientMarkedVoided() {
        Subject deleted = new Subject();
        deleted.setVoided(true);
        when(avniHttpClient.get("/api/subject/p1", Subject.class)).thenReturn(ResponseEntity.ok(deleted));

        assertTrue(repository.getSubjectOrThrow("p1").getVoided());
    }

    @Test
    public void getSubjectOrThrowThrowsWhenTheReadIsRefused() {
        when(avniHttpClient.get("/api/subject/p1", Subject.class))
                .thenThrow(HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden", HttpHeaders.EMPTY, null, null));

        assertThrows(HttpClientErrorException.Forbidden.class, () -> repository.getSubjectOrThrow("p1"));
        assertNull(repository.getSubject("p1"), "the older read turns the same refusal into a missing patient");
    }

    @Test
    public void getSubjectOrThrowThrowsOnNotFoundAndOnServerError() {
        when(avniHttpClient.get("/api/subject/p1", Subject.class))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, null, null))
                .thenThrow(HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", HttpHeaders.EMPTY, null, null));

        assertThrows(HttpClientErrorException.NotFound.class, () -> repository.getSubjectOrThrow("p1"));
        assertThrows(HttpServerErrorException.InternalServerError.class, () -> repository.getSubjectOrThrow("p1"));
    }
}
