package org.avni_integration_service.tanuh.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.apache.log4j.Logger;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// Hands one screening's photos to Tanuh's model service (POST /v1/cases) with a Google ID token. Only the answers the
// service's guide documents are trusted: 200 or 202 naming the case and a status is an acceptance; 409 for a case held
// with other photos and 422 for a case that breaks a rule are answers to keep; everything else, including an answer that
// is not what the guide says, is a HighRiskModelException so the screening waits and is sent again.
@Component
public class OcshrClient {
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    static final Duration READ_TIMEOUT = Duration.ofSeconds(60);
    private static final int DETAIL_LENGTH = 300;
    private static final Set<String> LOOPBACK = Set.of("localhost", "127.0.0.1", "[::1]");
    private static final Logger logger = Logger.getLogger(OcshrClient.class);

    private final TanuhContextProvider tanuhContextProvider;
    private final IdTokenSource idTokenSource;
    private final RestTemplate restTemplate;

    @Autowired
    public OcshrClient(TanuhContextProvider tanuhContextProvider, IdTokenSource idTokenSource) {
        this(tanuhContextProvider, idTokenSource,
                new RestTemplateBuilder().setConnectTimeout(CONNECT_TIMEOUT).setReadTimeout(READ_TIMEOUT).build());
    }

    OcshrClient(TanuhContextProvider tanuhContextProvider, IdTokenSource idTokenSource, RestTemplate restTemplate) {
        this.tanuhContextProvider = tanuhContextProvider;
        this.idTokenSource = idTokenSource;
        this.restTemplate = restTemplate;
    }

    // A token expires after an hour, so one 401 is answered with a fresh one and the same body is sent again. Messages
    // and logs carry the case id, the status and the cut body, never the token.
    public Submission submit(String caseId, List<String> imageUris) throws HighRiskModelException {
        TanuhConfig config = tanuhContextProvider.get();
        String baseUrl = config.getOcshrApiUrl();
        if (baseUrl == null)
            throw new HighRiskModelException("ocshr_api_url is not set for this organisation, so no screening can be sent to the model service");
        URI uri = URI.create(baseUrl + "/v1/cases");
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !LOOPBACK.contains(String.valueOf(uri.getHost()).toLowerCase(Locale.ROOT)))
            throw new HighRiskModelException(String.format("ocshr_api_url %s is not https, so nothing is sent: the Google ID token would travel in the clear", baseUrl));
        String json = body(caseId, imageUris);
        Answer answer = post(uri, json, idTokenSource.idToken(config));
        if (answer.status == 401) {
            logger.info(String.format("Model service refused the token for case %s; asking for a new one", caseId));
            answer = post(uri, json, idTokenSource.renewedIdToken(config));
        }
        Submission submission = interpret(caseId, answer);
        logger.info(String.format("Model service answered %d for case %s with %d photos: %s", answer.status, caseId,
                imageUris.size(), submission.outcome() == Submission.Outcome.ACCEPTED ? "accepted" : cut(answer.body)));
        return submission;
    }

    private static String body(String caseId, List<String> imageUris) {
        List<Map<String, String>> images = new ArrayList<>();
        for (String uri : imageUris) images.add(Map.of("uri", uri));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("case_id", caseId);
        body.put("images", images);
        return ObjectJsonMapper.writeValueAsString(body);
    }

    private record Answer(int status, String body) {
    }

    private Answer post(URI uri, String json, String token) throws HighRiskModelException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, new HttpEntity<>(json, headers), String.class);
            return new Answer(response.getStatusCodeValue(), response.getBody());
        } catch (HttpStatusCodeException e) {
            return new Answer(e.getRawStatusCode(), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            throw new HighRiskModelException(String.format("Model service at %s did not answer: %s", uri.getHost(), e.getMessage()), e);
        }
    }

    private static Submission interpret(String caseId, Answer answer) throws HighRiskModelException {
        switch (answer.status) {
            case 200, 202 -> {
                JsonNode body = json(answer.body);
                if (body.isObject() && caseId.equals(body.path("case_id").asText(null))
                        && body.path("status").isTextual() && !body.path("status").asText().isBlank())
                    return new Submission(Submission.Outcome.ACCEPTED, cut(answer.body));
                throw new HighRiskModelException(String.format("Model service answered %d for case %s but not with that case and a status: %s", answer.status, caseId, cut(answer.body)));
            }
            case 409 -> {
                if ("CASE_EXISTS_WITH_DIFFERENT_MANIFEST".equals(json(answer.body).path("error").asText(null)))
                    return new Submission(Submission.Outcome.CONFLICT, cut(answer.body));
            }
            case 422 -> {
                if ("INVALID_MANIFEST".equals(json(answer.body).path("error").asText(null)))
                    return new Submission(Submission.Outcome.REFUSED, cut(answer.body));
            }
            default -> {
            }
        }
        throw new HighRiskModelException(String.format("Model service answered %d for case %s: %s", answer.status, caseId, cut(answer.body)));
    }

    private static JsonNode json(String body) {
        try {
            JsonNode node = ObjectJsonMapper.objectMapper.readTree(body == null ? "" : body);
            return node == null ? MissingNode.getInstance() : node;
        } catch (Exception e) {
            return MissingNode.getInstance();
        }
    }

    private static String cut(String body) {
        if (body == null) return null;
        return body.length() > DETAIL_LENGTH ? body.substring(0, DETAIL_LENGTH) : body;
    }
}
