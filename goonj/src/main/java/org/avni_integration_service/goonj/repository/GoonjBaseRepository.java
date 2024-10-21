package org.avni_integration_service.goonj.repository;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.goonj.config.GoonjContextProvider;
import org.avni_integration_service.goonj.util.DateTimeUtil;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.avni_integration_service.util.ObjectJsonMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.client.*;

import java.net.URI;
import java.util.*;

public abstract class GoonjBaseRepository {
    private static final Logger logger = Logger.getLogger(GoonjBaseRepository.class);
    private static final String DELETION_RECORD_ID = "recordId";
    private static final String DELETION_SOURCE_ID = "sourceId";
    private static final String EMPTY_STRING = "";
    private static final String FILTER_KEY_STATE = "state";
    private static final String FILTER_KEY_ACCOUNT = "account";
    private static final String FILTER_KEY_TIMESTAMP = "dateTimestamp";
    private static final String API_PARAMS_DELIMITER = "&";
    private static final String FILTER_PARAM_FORMAT = "%s=%s";

    private final IntegratingEntityStatusRepository integratingEntityStatusRepository;
    private final RestTemplate goonjRestTemplate;
    private final String entityType;
    protected final AvniHttpClient avniHttpClient;
    protected final GoonjContextProvider goonjContextProvider;

    public GoonjBaseRepository(IntegratingEntityStatusRepository integratingEntityStatusRepository,
                               RestTemplate restTemplate, String entityType,
                               AvniHttpClient avniHttpClient, GoonjContextProvider goonjContextProvider) {
        this.integratingEntityStatusRepository = integratingEntityStatusRepository;
        this.goonjRestTemplate = restTemplate;
        this.entityType = entityType;
        this.avniHttpClient = avniHttpClient;
        this.goonjContextProvider = goonjContextProvider;
    }


    protected <T> T getResponseEntity(String resource, HashMap<String, String> queryParams, Class<T> returnType) {
        ResponseEntity<T> responseEntity = avniHttpClient.get(resource, queryParams, returnType);
        if(responseEntity.getStatusCode().is2xxSuccessful()) {
            return responseEntity.getBody();
        }
        logger.error(String.format("Failed to fetch resource %s, response status code is %s", resource, responseEntity.getStatusCode()));
        throw new HttpServerErrorException(responseEntity.getStatusCode());

    }

    protected <T> T getResponse(String resource,  Class<T> returnType, String filters) {
        URI uri = URI.create(String.format("%s/%s?%s", goonjContextProvider.get().getAppUrl(), resource,
                filters));
        ResponseEntity<T> responseEntity = goonjRestTemplate.exchange(uri, HttpMethod.GET, null, returnType);
        if(responseEntity.getStatusCode().is2xxSuccessful()) {
            return responseEntity.getBody();
        }
        logger.error(String.format("Failed to fetch data for resource %s, response status code is %s", resource, responseEntity.getStatusCode()));
        throw new HttpServerErrorException(responseEntity.getStatusCode());
    }

    /**
     * All our Sync time-stamps for Goonj, i.e. Demand, Dispatch, Distro, DispatchReceipt and Activity
     * are stored using integrating_entity_status DB and none in avniEntityStatus table.
     * @return cutOffDate
     */
    protected Date getCutOffDate() {
        return integratingEntityStatusRepository.findByEntityType(entityType).getReadUptoDateTime();
    }

    protected Date getCutOffDateTime() {
        return getCutOffDate();
    }

    protected String getAPIFilters(String dateTimeParam, Date cutoffDateTime, @NonNull Map<String, Object> filters) {
        Object taskDateTimeFilter = filters.getOrDefault(FILTER_KEY_TIMESTAMP, cutoffDateTime);
        Object stateFilterValue = filters.getOrDefault(FILTER_KEY_STATE, EMPTY_STRING);
        Object accountFilterValue = filters.getOrDefault(FILTER_KEY_ACCOUNT, EMPTY_STRING);
        Date dateTimeValue = Objects.nonNull(taskDateTimeFilter) ? (Date) taskDateTimeFilter : cutoffDateTime;
        String dateTimeOffsetFilter=String.format(FILTER_PARAM_FORMAT, dateTimeParam, DateTimeUtil.formatDateTime(dateTimeValue));
        String stateFilter=String.format(FILTER_PARAM_FORMAT, FILTER_KEY_STATE, stateFilterValue);
        String accountFilter=String.format(FILTER_PARAM_FORMAT, FILTER_KEY_ACCOUNT, accountFilterValue);
        return String.join(API_PARAMS_DELIMITER, dateTimeOffsetFilter, stateFilter, accountFilter);
    }

    protected <T> T getSingleEntityResponse(String resource, String filter, String uuid, Class<T> returnType) {
        URI uri = URI.create(String.format("%s/%s?%s=%s", goonjContextProvider.get().getAppUrl(), resource, filter, uuid));
        ResponseEntity<T> responseEntity = goonjRestTemplate.exchange(uri, HttpMethod.GET, null, returnType);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            return responseEntity.getBody();
        }
        logger.error(String.format("Failed to fetch data for resource %s, response status code is %s", resource, responseEntity.getStatusCode()));
        throw getRestClientResponseException(responseEntity.getHeaders(), responseEntity.getStatusCode(), null);
    }

    protected HashMap<String, Object>[] createSingleEntity(String resource, HttpEntity<?> requestEntity) throws RestClientResponseException {
        logger.info("Request body:" + ObjectJsonMapper.writeValueAsString(requestEntity.getBody()));
        URI uri = URI.create(String.format("%s/%s", goonjContextProvider.get().getAppUrl(), resource));
        ParameterizedTypeReference<HashMap<String, Object>[]> responseType = new ParameterizedTypeReference<>() {};
        ResponseEntity<HashMap<String, Object>[]> responseEntity = goonjRestTemplate.exchange(uri, HttpMethod.POST, requestEntity, responseType);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            return responseEntity.getBody();
        }
        logger.error(String.format("Failed to create resource %s,  response status code is %s", resource, responseEntity.getStatusCode()));
        throw handleError(responseEntity, responseEntity.getStatusCode());
    }

    protected Object deleteSingleEntity(String resource, HttpEntity<?> requestEntity) throws RestClientResponseException {
        logger.info("Request body:" + ObjectJsonMapper.writeValueAsString(requestEntity.getBody()));
        URI uri = URI.create(String.format("%s/%s", goonjContextProvider.get().getAppUrl(), resource));
        ParameterizedTypeReference<Object> responseType = new ParameterizedTypeReference<>() {};
        ResponseEntity<Object> responseEntity = goonjRestTemplate.exchange(uri, HttpMethod.POST, requestEntity, responseType);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            return responseEntity.getBody();
        }
        logger.error(String.format("Failed to delete resource %s, response error message is %s", resource, responseEntity.getBody()));
        throw getRestClientResponseException(responseEntity.getHeaders(), responseEntity.getStatusCode(), (String) responseEntity.getBody());
    }

    protected RestClientException handleError(ResponseEntity<HashMap<String, Object>[]> responseEntity, HttpStatus statusCode) {
        HashMap<String, Object>[] responseBody = responseEntity.getBody();
        String message = (String) responseBody[0].get("message");
        return getRestClientResponseException(responseEntity.getHeaders(), statusCode, message);
    }

    private RestClientResponseException getRestClientResponseException(HttpHeaders headers, HttpStatus statusCode, String message) {
        return switch (statusCode.series()) {
            case CLIENT_ERROR -> HttpClientErrorException.create(message, statusCode, null, headers, null, null);
            case SERVER_ERROR -> HttpServerErrorException.create(message, statusCode, null, headers, null, null);
            default -> new UnknownHttpStatusCodeException(message, statusCode.value(), null, headers, null, null);
        };
    }

    private HttpEntity<Map<String, List>> getDeleteEncounterHttpRequestEntity(GeneralEncounter encounter) {
        Map<String, List> deleteRequest = Map.of(DELETION_RECORD_ID, new ArrayList(), DELETION_SOURCE_ID, Arrays.asList(encounter.getUuid()) );
        HttpEntity<Map<String, List>> requestEntity = new HttpEntity<>(deleteRequest);
        return requestEntity;
    }

    public Object deleteEvent(String resourceType, GeneralEncounter encounter) {
        HttpEntity<Map<String, List>> requestEntity = getDeleteEncounterHttpRequestEntity(encounter);
        return deleteSingleEntity(resourceType, requestEntity);
    }

    public boolean wasEventCreatedSuccessfully(HashMap<String, Object>[] response) {
        return (response != null && response[0].get("errorCode") == null);
    }

    public abstract HashMap<String, Object>[] fetchEvents(Map<String, Object> filters);
    public abstract List<String> fetchDeletionEvents(Map<String, Object> filters);
    public abstract HashMap<String, Object>[] createEvent(Subject subject);
    public abstract HashMap<String, Object>[] createEvent(Subject subject, GeneralEncounter encounter);
}
