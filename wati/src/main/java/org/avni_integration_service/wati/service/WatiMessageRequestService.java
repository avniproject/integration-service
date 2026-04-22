package org.avni_integration_service.wati.service;

import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.avni_integration_service.wati.config.WatiContextProvider;
import org.avni_integration_service.wati.config.WatiFlowConfig;
import org.avni_integration_service.wati.domain.WatiMessageRequest;
import org.avni_integration_service.wati.domain.WatiMessageStatus;
import org.avni_integration_service.wati.repository.WatiMessageRequestRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class WatiMessageRequestService {

    private final WatiMessageRequestRepository watiMessageRequestRepository;
    private final IntegrationSystemRepository integrationSystemRepository;
    private final WatiContextProvider watiContextProvider;

    public WatiMessageRequestService(WatiMessageRequestRepository watiMessageRequestRepository,
                                     IntegrationSystemRepository integrationSystemRepository,
                                     WatiContextProvider watiContextProvider) {
        this.watiMessageRequestRepository = watiMessageRequestRepository;
        this.integrationSystemRepository = integrationSystemRepository;
        this.watiContextProvider = watiContextProvider;
    }

    public boolean isInCooldown(String entityId, String flowName, int cooldownDays) {
        List<WatiMessageStatus> activeStatuses = Arrays.asList(
                WatiMessageStatus.Pending, WatiMessageStatus.Sent, WatiMessageStatus.Delivered);
        return watiMessageRequestRepository
                .existsByEntityIdAndFlowNameAndStatusInAndCreatedDateTimeAfterAndIntegrationSystem_Id(
                        entityId, flowName, activeStatuses,
                        LocalDateTime.now().minusDays(cooldownDays),
                        watiContextProvider.get().getIntegrationSystem().getId());
    }

    public WatiMessageRequest createRequest(String phoneNumber, String locale, String entityId,
                                            String templateName, WatiFlowConfig flowConfig) {
        IntegrationSystem integrationSystem = integrationSystemRepository.findEntity(
                watiContextProvider.get().getIntegrationSystem().getId());
        WatiMessageRequest request = new WatiMessageRequest();
        request.setIntegrationSystem(integrationSystem);
        request.setFlowName(flowConfig.getFlowName());
        request.setEntityId(entityId);
        request.setEntityType("encounter");
        request.setPhoneNumber(phoneNumber);
        request.setTemplateName(templateName);
        request.setLocale(locale);
        request.setNextRetryTime(LocalDateTime.now());
        return watiMessageRequestRepository.save(request);
    }

    public void markSent(WatiMessageRequest request, String watiMessageId) {
        request.setStatus(WatiMessageStatus.Sent);
        request.setWatiMessageId(watiMessageId);
        request.setLastAttemptTime(LocalDateTime.now());
        watiMessageRequestRepository.save(request);
    }

    public void markFailed(WatiMessageRequest request, String errorMessage, int maxRetries, int retryIntervalHours) {
        request.setAttemptCount(request.getAttemptCount() + 1);
        request.setLastAttemptTime(LocalDateTime.now());
        request.setErrorMessage(errorMessage);
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
        request.setLastAttemptTime(LocalDateTime.now());
        watiMessageRequestRepository.save(request);
    }

    public List<WatiMessageRequest> getPendingRequests() {
        return watiMessageRequestRepository.findByIntegrationSystem_IdAndStatusAndNextRetryTimeLessThanEqual(
                watiContextProvider.get().getIntegrationSystem().getId(),
                WatiMessageStatus.Pending, LocalDateTime.now());
    }

    public List<WatiMessageRequest> getFailedRequestsDueForRetry() {
        return watiMessageRequestRepository.findByIntegrationSystem_IdAndStatusAndNextRetryTimeLessThanEqual(
                watiContextProvider.get().getIntegrationSystem().getId(),
                WatiMessageStatus.Failed, LocalDateTime.now());
    }
}
