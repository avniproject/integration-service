package org.avni_integration_service.avni.client;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.auth.IdpDetailsResponse;
import org.avni_integration_service.util.ObjectJsonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Component
public class AvniHttpClient {
    @Autowired
    @Qualifier("AvniRestTemplate")
    private RestTemplate restTemplate;

    private static final Logger logger = Logger.getLogger(AvniHttpClient.class);

    private static ThreadLocal<AvniSession> avniSessions = new ThreadLocal<>();

    public void setAvniSession(AvniSession avniSession) {
        avniSessions.set(avniSession);
    }

    AvniSession getAvniSession() {
        AvniSession avniSession = avniSessions.get();
        if (avniSession == null)
            throw new IllegalStateException("No Avni connection available. Have you called setAvniConnectionDetails.");
        return avniSession;
    }

    public <T> ResponseEntity<T> get(String path, Map<String, String> queryParams, Class<T> returnType) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(getAvniSession().apiUrl(path));
        for (var entry : queryParams.entrySet()) {
            builder.queryParam(entry.getKey(), entry.getValue());
        }

        URI uri = builder.build().toUri();
        return getResponseEntity(returnType, uri, HttpMethod.GET, null);
    }

    private <T> ResponseEntity<T> getResponseEntity(Class<T> returnType, URI uri, HttpMethod method, String json) {
        try {
            logger.debug("%s %s".formatted(method.name(), uri.toString()));
            return restTemplate.exchange(uri, method, getRequestEntity(json), returnType);
        } catch (HttpServerErrorException.InternalServerError e) {
            if (e.getMessage().contains("TokenExpiredException")) {
                getAvniSession().clearAuthInformation();
                return restTemplate.exchange(uri, method, getRequestEntity(json), returnType);
            }
            logger.error(String.format("URI: %s, Errored Request body: %s", uri, json));
            throw e;
        } catch (Exception e) {
            logger.error(String.format("URI: %s, Errored Request body: %s", uri, json));
            throw e;
        }
    }

    private HttpEntity<String> getRequestEntity(String json) {
        return json == null ? new HttpEntity<>(authHeaders()) : new HttpEntity<>(json, authHeaders());
    }

    public <T> ResponseEntity<T> get(String url, Class<T> returnType) {
        return get(url, new HashMap<>(), returnType);
    }

    public <T, U> ResponseEntity<U> post(String url, T requestBody, Class<U> returnType) {
        logger.info(String.format("POST: %s", url));
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(getAvniSession().apiUrl(url));
        String json = ObjectJsonMapper.writeValueAsString(requestBody);
        return getResponseEntity(returnType, builder.build().toUri(), HttpMethod.POST, json);
    }

    public <T, U> ResponseEntity<U> put(String url, T requestBody, Class<U> returnType) {
        logger.info(String.format("PUT: %s", url));
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(getAvniSession().apiUrl(url));
        String json = ObjectJsonMapper.writeValueAsString(requestBody);
        return getResponseEntity(returnType, builder.build().toUri(), HttpMethod.PUT, json);
    }

    public <T> ResponseEntity<T> delete(String url,  Map<String, String> queryParams, String json, Class<T> returnType) {
        logger.info(String.format("DELETE: %s", url));
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(getAvniSession().apiUrl(url));
        for (var entry : queryParams.entrySet()) {
            builder.queryParam(entry.getKey(), entry.getValue());
        }
        return getResponseEntity(returnType, builder.build().toUri(), HttpMethod.DELETE, json);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (!getAvniSession().getAuthWithAvni()) { //Skip Cognito Auth Token fetch in local
            headers.add("user-name", getAvniSession().getAvniImplUser());
        } else {
            headers.add("auth-token", fetchAuthToken());
        }
        headers.add("content-type", "application/json");
        return headers;
    }

    String fetchAuthToken() {
        String idToken = getAvniSession().getIdToken();
        if (idToken != null) return idToken;

        RestTemplate restTemplate = new RestTemplate();
        logger.debug("Getting cognito details");
        ResponseEntity<IdpDetailsResponse> response = restTemplate.getForEntity(getAvniSession().apiUrl("/idp-details"), IdpDetailsResponse.class);
        IdpDetailsResponse idpDetailsResponse = response.getBody();
        return getAvniSession().fetchIdToken(idpDetailsResponse);
    }

    public String getUri(String url, HashMap<String, String> queryParams) {
        return getAvniSession().getUri(url, queryParams);
    }
}
