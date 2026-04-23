package org.avni_integration_service.wati.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.MessageDeliveryStatus;
import org.avni_integration_service.avni.domain.SendMessageResponse;
import org.avni_integration_service.wati.config.WatiContextProvider;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WatiHttpClient {

    private static final Logger logger = Logger.getLogger(WatiHttpClient.class);

    private final WatiContextProvider watiContextProvider;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WatiHttpClient(WatiContextProvider watiContextProvider) {
        this.watiContextProvider = watiContextProvider;
        this.restTemplate = new RestTemplate();
    }

    public SendMessageResponse sendTemplateMessage(String phoneNumber, String templateName, String parametersJson) {
        String apiUrl = watiContextProvider.get().getWatiApiUrl();
        String apiKey = watiContextProvider.get().getWatiApiKey();
        String url = apiUrl + "/api/v1/sendTemplateMessages";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");

        Map<String, Object> receiver = new HashMap<>();
        receiver.put("whatsappNumber", phoneNumber);
        receiver.put("customParams", parseParameters(parametersJson));

        String broadcastName = templateName + "_" + LocalDate.now();
        Map<String, Object> body = new HashMap<>();
        body.put("template_name", templateName);
        body.put("broadcast_name", broadcastName);
        body.put("receivers", List.of(receiver));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            logger.info(String.format("Sending Wati template '%s' to phone %s", templateName, phoneNumber));
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object result = response.getBody().get("result");
                if (Boolean.TRUE.equals(result)) {
                    logger.info(String.format("Wati message sent to %s, broadcastName: %s", phoneNumber, broadcastName));
                    return new SendMessageResponse(MessageDeliveryStatus.Sent, null, broadcastName);
                }
                String error = String.valueOf(response.getBody().get("info"));
                logger.warn(String.format("Wati API result=false for phone %s: %s", phoneNumber, error));
                return new SendMessageResponse(MessageDeliveryStatus.NotSent, error);
            }
            logger.error(String.format("Wati API returned status %s for phone %s", response.getStatusCode(), phoneNumber));
            return new SendMessageResponse(MessageDeliveryStatus.Failed, "HTTP " + response.getStatusCode());
        } catch (HttpClientErrorException e) {
            logger.error(String.format("Wati API client error for phone %s: %s", phoneNumber, e.getMessage()));
            return new SendMessageResponse(MessageDeliveryStatus.NotSent, e.getMessage());
        } catch (HttpServerErrorException e) {
            logger.error(String.format("Wati API server error for phone %s: %s", phoneNumber, e.getMessage()));
            return new SendMessageResponse(MessageDeliveryStatus.Failed, e.getMessage());
        }
    }


    private List<Map<String, String>> parseParameters(String parametersJson) {
        if (!StringUtils.hasLength(parametersJson)) return new ArrayList<>();
        try {
            return objectMapper.readValue(parametersJson, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            logger.warn("Failed to parse parameters JSON: " + parametersJson);
            return new ArrayList<>();
        }
    }
}
