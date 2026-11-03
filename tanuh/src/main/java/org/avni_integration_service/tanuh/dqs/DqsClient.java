package org.avni_integration_service.tanuh.dqs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.tanuh.config.TanuhConfig;
import org.avni_integration_service.tanuh.config.TanuhContextProvider;
import org.avni_integration_service.util.ObjectJsonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// Posts one record at a time to the data quality service's route for its record type, as the organisation's Avni job
// user: the service checks the job user's Cognito ID token, the same one the Avni calls carry. Every answer other than
// "stored" is a DqsException, so the screening waits in an error record and is sent again by the error job.
@Component
public class DqsClient {
    public static final String ORAL_SCREENING = "oral_screening";
    public static final String REGISTRATION = "registration";
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    static final Duration READ_TIMEOUT = Duration.ofSeconds(60);
    private static final int DETAIL_LENGTH = 300;
    private static final Set<String> LOOPBACK = Set.of("localhost", "127.0.0.1", "[::1]");
    private static final Logger logger = Logger.getLogger(DqsClient.class);

    private final AvniHttpClient avniHttpClient;
    private final TanuhContextProvider tanuhContextProvider;
    private final RestTemplate restTemplate;

    @Autowired
    public DqsClient(AvniHttpClient avniHttpClient, TanuhContextProvider tanuhContextProvider) {
        this(avniHttpClient, tanuhContextProvider,
                new RestTemplateBuilder().setConnectTimeout(CONNECT_TIMEOUT).setReadTimeout(READ_TIMEOUT).build());
    }

    DqsClient(AvniHttpClient avniHttpClient, TanuhContextProvider tanuhContextProvider, RestTemplate restTemplate) {
        this.avniHttpClient = avniHttpClient;
        this.tanuhContextProvider = tanuhContextProvider;
        this.restTemplate = restTemplate;
    }

    // The payload is never logged: a registration carries the patient's name and address, which the service hashes
    // or drops on arrival. A token expires after an hour, so one 401 is answered with a fresh sign-in.
    public DqsResult submit(String recordType, Map<String, Object> payload) {
        TanuhConfig config = tanuhContextProvider.get();
        if (config.getDqsApiUrl() == null)
            throw new DqsException("dqs_api_url is not set for this organisation, so no record can be sent");
        URI uri = URI.create(String.format("%s/v1/%s/records/%s", config.getDqsApiUrl(), config.getDqsTenant(), recordType));
        // The job user's sign-in token goes with every record, so it is never sent in the clear, except to a service on
        // this machine.
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !LOOPBACK.contains(String.valueOf(uri.getHost()).toLowerCase(Locale.ROOT)))
            throw new DqsException(String.format("dqs_api_url %s is not https, so no record is sent: the job user's token would travel in the clear", config.getDqsApiUrl()));
        String recordId = String.valueOf(payload.get("record_id"));
        String json = ObjectJsonMapper.writeValueAsString(payload);
        String token = avniHttpClient.getIdToken();
        Answer answer = post(uri, json, token);
        if (answer.status == 401 && token != null) {
            logger.info(String.format("DQS refused the token for %s %s; signing in again", recordType, recordId));
            answer = post(uri, json, avniHttpClient.renewIdToken());
        }
        return interpret(recordType, recordId, config.getDqsTenant(), answer);
    }

    private record Answer(int status, String body) {
    }

    private Answer post(URI uri, String json, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) headers.setBearerAuth(token);
        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, new HttpEntity<>(json, headers), String.class);
            return new Answer(response.getStatusCodeValue(), response.getBody());
        } catch (HttpStatusCodeException e) {
            return new Answer(e.getRawStatusCode(), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            throw new DqsException(String.format("DQS at %s did not answer: %s", uri.getHost(), e.getMessage()), e);
        }
    }

    private static DqsResult interpret(String recordType, String recordId, String tenant, Answer answer) {
        String what = String.format("%s %s", recordType, recordId);
        switch (answer.status) {
            case 200, 422 -> {
                return stored(what, answer);
            }
            case 400 -> throw new DqsException(String.format("DQS could not read the request for %s (400): %s", what, detail(answer)));
            case 401 -> throw new DqsException(String.format("DQS refused the job user's token for %s (401): %s", what, detail(answer)));
            case 403 -> throw new DqsException(String.format("DQS does not allow the job user for tenant %s (403): %s. Add the user's Cognito subject to the tenant's authentication file.", tenant, detail(answer)));
            case 404 -> throw new DqsException(String.format("DQS has no route for %s (404): check dqs_api_url, dqs_tenant and that the tenant's published pack carries %s", what, recordType));
            case 503 -> throw new DqsException(String.format("DQS cannot take %s right now (503): %s", what, detail(answer)));
            default -> throw new DqsException(String.format("DQS answered %d for %s: %s", answer.status, what, detail(answer)));
        }
    }

    // A 200 passes or fails the record only on an answer that says how it was scored: evaluation.scored and whole
    // counts under summary.outcomes. Anything else, such as a proxy's page, is a DqsException rather than a pass with no
    // failed rules, so the screening waits and is sent again. A 422 is a record the service could not read, a fail
    // whatever its body holds.
    private static DqsResult stored(String what, Answer answer) {
        JsonNode body;
        try {
            body = ObjectJsonMapper.objectMapper.readTree(answer.body == null ? "" : answer.body);
        } catch (Exception e) {
            body = MissingNode.getInstance();
        }
        if (body == null) body = MissingNode.getInstance();
        JsonNode evaluation = body.path("evaluation");
        JsonNode counts = body.path("summary").path("outcomes");
        if (answer.status == 200 && !(evaluation.path("scored").isBoolean() && counts.isObject() && allWhole(counts)))
            throw new DqsException(String.format("DQS stored %s but its answer does not say how it was scored: %s", what, detail(answer)));
        boolean scored = evaluation.path("scored").isBoolean() ? evaluation.path("scored").asBoolean() : answer.status == 200;
        List<String> violations = new ArrayList<>();
        for (JsonNode violation : evaluation.path("violations"))
            violations.add(String.format("%s %s: %s", violation.path("level").asText(), violation.path("code").asText(), violation.path("message").asText()));
        Map<String, Integer> outcomes = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = counts.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> count = fields.next();
            outcomes.put(count.getKey(), count.getValue().asInt());
        }
        return new DqsResult(answer.status, scored, violations, outcomes);
    }

    private static boolean allWhole(JsonNode counts) {
        Iterator<JsonNode> values = counts.elements();
        while (values.hasNext()) if (!values.next().canConvertToExactIntegral()) return false;
        return true;
    }

    private static String detail(Answer answer) {
        if (answer.body == null || answer.body.isBlank()) return "(no detail)";
        try {
            JsonNode detail = ObjectJsonMapper.objectMapper.readTree(answer.body).path("detail");
            if (detail.isTextual()) return detail.asText();
        } catch (Exception ignored) {
            // not JSON: the body itself is the detail
        }
        return answer.body.length() > DETAIL_LENGTH ? answer.body.substring(0, DETAIL_LENGTH) : answer.body;
    }
}
