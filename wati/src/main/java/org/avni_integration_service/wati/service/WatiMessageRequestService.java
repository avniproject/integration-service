package org.avni_integration_service.wati.service;

import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.avni_integration_service.wati.config.WatiContextProvider;
import org.avni_integration_service.wati.config.WatiFlowConfig;
import org.avni_integration_service.wati.domain.WatiMessageRequest;
import org.avni_integration_service.wati.domain.WatiMessageStatus;
import org.avni_integration_service.wati.repository.WatiMessageRequestRepository;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
public class WatiMessageRequestService {

    private static final int SEND_BATCH_SIZE = 500;

    private final WatiMessageRequestRepository watiMessageRequestRepository;
    private final IntegrationSystemRepository integrationSystemRepository;
    private final WatiContextProvider watiContextProvider;

    // C1: cached per job run to avoid one DB round-trip per row in createRequest
    private IntegrationSystem cachedIntegrationSystem;

    public WatiMessageRequestService(WatiMessageRequestRepository watiMessageRequestRepository,
                                     IntegrationSystemRepository integrationSystemRepository,
                                     WatiContextProvider watiContextProvider) {
        this.watiMessageRequestRepository = watiMessageRequestRepository;
        this.integrationSystemRepository = integrationSystemRepository;
        this.watiContextProvider = watiContextProvider;
    }

    private IntegrationSystem getIntegrationSystem() {
        int currentId = watiContextProvider.get().getIntegrationSystem().getId();
        if (cachedIntegrationSystem == null || cachedIntegrationSystem.getId() != currentId) {
            cachedIntegrationSystem = integrationSystemRepository.findEntity(currentId);
        }
        return cachedIntegrationSystem;
    }

    public boolean isInCooldown(String entityId, String flowName, int cooldownDays) {
        List<WatiMessageStatus> activeStatuses = Arrays.asList(
                WatiMessageStatus.Pending, WatiMessageStatus.Sending, WatiMessageStatus.Sent,
                WatiMessageStatus.Failed);
        return watiMessageRequestRepository
                .existsByEntityIdAndFlowNameAndStatusInAndCreatedDateTimeAfterAndIntegrationSystem_Id(
                        entityId, flowName, activeStatuses,
                        LocalDateTime.now().minusDays(cooldownDays),
                        watiContextProvider.get().getIntegrationSystem().getId());
    }

    public Set<String> getCooldownEntityIds(String flowName, int cooldownDays) {
        List<WatiMessageStatus> activeStatuses = Arrays.asList(
                WatiMessageStatus.Pending, WatiMessageStatus.Sending,
                WatiMessageStatus.Sent, WatiMessageStatus.Failed);
        return watiMessageRequestRepository.findEntityIdsInCooldown(
                flowName, activeStatuses,
                LocalDateTime.now().minusDays(cooldownDays),
                watiContextProvider.get().getIntegrationSystem().getId());
    }

    public WatiMessageRequest createRequest(String phoneNumber, String locale, String entityId,
                                            String templateName, String parametersJson, WatiFlowConfig flowConfig) {
        WatiMessageRequest request = new WatiMessageRequest();
        request.setIntegrationSystem(getIntegrationSystem());
        request.setFlowName(flowConfig.getFlowName());
        request.setEntityId(entityId);
        request.setEntityType(flowConfig.getEntityType());
        request.setPhoneNumber(phoneNumber);
        request.setTemplateName(templateName);
        request.setParameters(parametersJson);
        request.setLocale(locale);
        request.setNextRetryTime(LocalDateTime.now());
        return watiMessageRequestRepository.save(request);
    }

    public WatiMessageRequest markSending(WatiMessageRequest request) {
        request.setStatus(WatiMessageStatus.Sending);
        request.setAttemptCount(request.getAttemptCount() + 1);
        request.setLastAttemptTime(LocalDateTime.now());
        return watiMessageRequestRepository.save(request);
    }

    public List<WatiMessageRequest> getStuckSendingRequests() {
        return watiMessageRequestRepository.findByIntegrationSystem_IdAndStatusAndLastAttemptTimeBefore(
                watiContextProvider.get().getIntegrationSystem().getId(),
                WatiMessageStatus.Sending,
                LocalDateTime.now().minusHours(1));
    }

    public void resetStuckTopending(WatiMessageRequest request) {
        request.setStatus(WatiMessageStatus.Pending);
        // markSending already incremented attemptCount; a crash before the response means the send
        // never completed, so roll the attempt back rather than burning a retry on the recovery.
        if (request.getAttemptCount() > 0) {
            request.setAttemptCount(request.getAttemptCount() - 1);
        }
        request.setNextRetryTime(LocalDateTime.now());
        watiMessageRequestRepository.save(request);
    }

    public void markSent(WatiMessageRequest request, String watiMessageId) {
        request.setStatus(WatiMessageStatus.Sent);
        request.setWatiMessageId(watiMessageId);
        request.setLastAttemptTime(LocalDateTime.now());
        watiMessageRequestRepository.save(request);
    }

    public void markFailed(WatiMessageRequest request, String errorMessage, int maxRetries, int retryIntervalHours) {
        request.setErrorMessage(errorMessage);
        // attemptCount is pre-incremented in markSending, so >= gives exactly maxRetries *total* attempts.
        if (request.getAttemptCount() >= maxRetries) {
            request.setStatus(WatiMessageStatus.PermanentFailure);
        } else {
            request.setStatus(WatiMessageStatus.Failed);
            request.setNextRetryTime(LocalDateTime.now().plusHours(retryIntervalHours));
        }
        watiMessageRequestRepository.save(request);
    }

    public void markPermanentFailure(WatiMessageRequest request, String errorMessage) {
        request.setStatus(WatiMessageStatus.PermanentFailure);
        request.setErrorMessage(errorMessage);
        watiMessageRequestRepository.save(request);
    }

    public List<WatiMessageRequest> getPendingRequests() {
        return watiMessageRequestRepository.findByIntegrationSystem_IdAndStatusAndNextRetryTimeLessThanEqual(
                watiContextProvider.get().getIntegrationSystem().getId(),
                WatiMessageStatus.Pending, LocalDateTime.now(),
                PageRequest.of(0, SEND_BATCH_SIZE));
    }

    public List<WatiMessageRequest> getFailedRequestsDueForRetry() {
        return watiMessageRequestRepository.findByIntegrationSystem_IdAndStatusAndNextRetryTimeLessThanEqual(
                watiContextProvider.get().getIntegrationSystem().getId(),
                WatiMessageStatus.Failed, LocalDateTime.now(),
                PageRequest.of(0, SEND_BATCH_SIZE));
    }
}
