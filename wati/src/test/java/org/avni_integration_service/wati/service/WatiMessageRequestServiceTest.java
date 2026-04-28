package org.avni_integration_service.wati.service;

import org.avni_integration_service.wati.domain.WatiMessageRequest;
import org.avni_integration_service.wati.domain.WatiMessageStatus;
import org.avni_integration_service.wati.repository.WatiMessageRequestRepository;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.avni_integration_service.wati.config.WatiContextProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class WatiMessageRequestServiceTest {

    @Mock private WatiMessageRequestRepository repository;
    @Mock private IntegrationSystemRepository integrationSystemRepository;
    @Mock private WatiContextProvider contextProvider;

    private WatiMessageRequestService service;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new WatiMessageRequestService(repository, integrationSystemRepository, contextProvider);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    // --- markSent ---

    @Test
    public void markSent_setsStatusAndMessageId() {
        WatiMessageRequest request = new WatiMessageRequest();
        service.markSent(request, "wati-msg-123");

        assertEquals(WatiMessageStatus.Sent, request.getStatus());
        assertEquals("wati-msg-123", request.getWatiMessageId());
        assertNotNull(request.getLastAttemptTime());
    }

    // --- markPermanentFailure ---

    @Test
    public void markPermanentFailure_setsStatusAndError() {
        WatiMessageRequest request = new WatiMessageRequest();
        service.markPermanentFailure(request, "phone not on WhatsApp");

        assertEquals(WatiMessageStatus.PermanentFailure, request.getStatus());
        assertEquals("phone not on WhatsApp", request.getErrorMessage());
        // lastAttemptTime is set by markSending before this is called, not here
    }

    // --- markFailed retry logic ---

    @Test
    public void markFailed_setsFailedStatusWhenBelowMaxRetries() {
        WatiMessageRequest request = new WatiMessageRequest();
        // attemptCount starts at 0; markSending increments it before markFailed is called
        service.markFailed(request, "Wati 500 error", 3, 24);

        assertEquals(WatiMessageStatus.Failed, request.getStatus());
        assertEquals(0, request.getAttemptCount()); // markFailed does not increment; markSending does
        assertNotNull(request.getNextRetryTime());
    }

    @Test
    public void markFailed_setsPermanentFailureWhenMaxRetriesReached() {
        WatiMessageRequest request = new WatiMessageRequest();
        // simulate 3 prior markSending calls having incremented count to maxRetries
        request.setAttemptCount(3);
        service.markFailed(request, "Wati 500 error", 3, 24);

        assertEquals(WatiMessageStatus.PermanentFailure, request.getStatus());
        assertEquals(3, request.getAttemptCount());
    }

    @Test
    public void markFailed_setsNextRetryTimeCorrectly() {
        WatiMessageRequest request = new WatiMessageRequest();
        service.markFailed(request, "timeout", 3, 24);

        // next retry should be ~24 hours from now
        assertTrue(request.getNextRetryTime().isAfter(
                java.time.LocalDateTime.now().plusHours(23)));
    }

    @Test
    public void newRequest_hasDefaultPendingStatus() {
        WatiMessageRequest request = new WatiMessageRequest();
        assertEquals(WatiMessageStatus.Pending, request.getStatus());
        assertEquals(0, request.getAttemptCount());
        assertNotNull(request.getCreatedDateTime());
    }
}
