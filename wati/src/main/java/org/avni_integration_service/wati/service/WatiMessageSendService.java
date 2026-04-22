package org.avni_integration_service.wati.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.MessageDeliveryStatus;
import org.avni_integration_service.avni.domain.SendMessageResponse;
import org.avni_integration_service.wati.client.WatiHttpClient;
import org.avni_integration_service.wati.config.WatiContextProvider;
import org.avni_integration_service.wati.config.WatiFlowConfig;
import org.avni_integration_service.wati.domain.WatiMessageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WatiMessageSendService {

    private static final Logger logger = Logger.getLogger(WatiMessageSendService.class);

    private final WatiHttpClient watiHttpClient;
    private final WatiMessageRequestService watiMessageRequestService;
    private final WatiContextProvider watiContextProvider;

    public WatiMessageSendService(WatiHttpClient watiHttpClient,
                                  WatiMessageRequestService watiMessageRequestService,
                                  WatiContextProvider watiContextProvider) {
        this.watiHttpClient = watiHttpClient;
        this.watiMessageRequestService = watiMessageRequestService;
        this.watiContextProvider = watiContextProvider;
    }

    public void sendPending() {
        List<WatiMessageRequest> pending = watiMessageRequestService.getPendingRequests();
        logger.info(String.format("Sending %d pending message requests", pending.size()));
        pending.forEach(this::send);
    }

    public void retryFailed() {
        List<WatiMessageRequest> failed = watiMessageRequestService.getFailedRequestsDueForRetry();
        logger.info(String.format("Retrying %d failed message requests", failed.size()));
        failed.forEach(this::send);
    }

    private void send(WatiMessageRequest request) {
        WatiFlowConfig flowConfig = watiContextProvider.get().getFlowConfig(request.getFlowName());
        try {
            SendMessageResponse response = watiHttpClient.sendTemplateMessage(
                    request.getPhoneNumber(), request.getTemplateName(), new String[0]);
            handleResponse(request, response, flowConfig);
        } catch (Exception e) {
            logger.error(String.format("Unexpected error sending request %s: %s", request.getId(), e.getMessage()), e);
            watiMessageRequestService.markFailed(request, e.getMessage(),
                    flowConfig.getMaxRetries(), flowConfig.getRetryIntervalHours());
        }
    }

    private void handleResponse(WatiMessageRequest request, SendMessageResponse response, WatiFlowConfig flowConfig) {
        if (response.getMessageDeliveryStatus() == MessageDeliveryStatus.Sent) {
            watiMessageRequestService.markSent(request, response.getMessageId());
            logger.info(String.format("Request %s sent, watiMessageId: %s", request.getId(), response.getMessageId()));
        } else if (response.getMessageDeliveryStatus() == MessageDeliveryStatus.NotSent) {
            watiMessageRequestService.markPermanentFailure(request, response.getErrorMessage());
            logger.warn(String.format("Permanent failure for request %s: %s", request.getId(), response.getErrorMessage()));
        } else {
            watiMessageRequestService.markFailed(request, response.getErrorMessage(),
                    flowConfig.getMaxRetries(), flowConfig.getRetryIntervalHours());
            logger.warn(String.format("Transient failure for request %s: %s", request.getId(), response.getErrorMessage()));
        }
    }
}
