package org.avni_integration_service.avni.repository;

import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.avni.domain.EncountersResponse;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.GeneralEncountersResponse;
import org.avni_integration_service.util.FormatAndParseUtil;
import org.avni_integration_service.util.ObjectJsonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class AvniEncounterRepository extends BaseAvniRepository {
    @Autowired
    private AvniHttpClient avniHttpClient;

    public GeneralEncounter getEncounter(HashMap<String, Object> concepts) {
        HashMap<String, String> queryParams = new HashMap<>();
        queryParams.put("concepts", ObjectJsonMapper.writeValueAsString(concepts));
        ResponseEntity<EncountersResponse> responseEntity = avniHttpClient.get("/api/encounters", queryParams, EncountersResponse.class);
        return pickAndExpectOne(responseEntity.getBody().getContent());
    }

    public GeneralEncounter getEncounter(String encounterType, Map<String, Object> concepts) {
        HashMap<String, String> queryParams = new HashMap<>();
        queryParams.put("concepts", ObjectJsonMapper.writeValueAsString(concepts));
        queryParams.put("encounterType", encounterType);
        ResponseEntity<EncountersResponse> responseEntity = avniHttpClient.get("/api/encounters", queryParams, EncountersResponse.class);
        return pickAndExpectOne(responseEntity.getBody().getContent());
    }

    public GeneralEncounter create(GeneralEncounter encounter) {
        ResponseEntity<GeneralEncounter> responseEntity = avniHttpClient.post("/api/encounter", encounter, GeneralEncounter.class);
        return responseEntity.getBody();
    }

    public GeneralEncounter update(String id, GeneralEncounter encounter) {
        ResponseEntity<GeneralEncounter> responseEntity = avniHttpClient.put(String.format("/api/encounter/%s", id), encounter, GeneralEncounter.class);
        return responseEntity.getBody();
    }

    // Sends only these observations, keyed by concept name. A null value removes that observation.
    public GeneralEncounter patch(String uuid, Map<String, Object> observations) {
        Map<String, Object> body = new HashMap<>();
        body.put("observations", observations);
        ResponseEntity<GeneralEncounter> responseEntity = avniHttpClient.patch(String.format("/api/encounter/%s", uuid), body, GeneralEncounter.class);
        return responseEntity.getBody();
    }

    public GeneralEncountersResponse getGeneralEncounters(@NonNull Date lastModifiedDateTime) {
        Map<String, String> queryParams = Map.of(
                "lastModifiedDateTime", FormatAndParseUtil.toISODateTimeString(lastModifiedDateTime),
                "size", "10");
        ResponseEntity<GeneralEncountersResponse> responseEntity = avniHttpClient.get("/api/encounters", queryParams, GeneralEncountersResponse.class);
        return responseEntity.getBody();
    }


    public GeneralEncountersResponse getGeneralEncounters(@NonNull  Date lastModifiedDateTime, @NonNull String encounterType) {
        Map<String, String> queryParams = Map.of(
                "encounterType", encounterType,
                "lastModifiedDateTime", FormatAndParseUtil.toISODateTimeString(lastModifiedDateTime),
                "size", "10");
        ResponseEntity<GeneralEncountersResponse> responseEntity = avniHttpClient.get("/api/encounters", queryParams, GeneralEncountersResponse.class);
        return responseEntity.getBody();
    }

    public static final int MAX_PAGE_SIZE = 1000;

    // Sends no "now": the server then reads up to 10 seconds ago, leaving room for transactions still committing.
    public GeneralEncountersResponse getGeneralEncounters(@NonNull Date lastModifiedDateTime, @NonNull String encounterType, int pageSize) {
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE)
            throw new IllegalArgumentException(String.format("Page size must be from 1 to %d, was %d", MAX_PAGE_SIZE, pageSize));
        Map<String, String> queryParams = Map.of(
                "encounterType", encounterType,
                "lastModifiedDateTime", FormatAndParseUtil.toISODateTimeString(lastModifiedDateTime),
                "size", String.valueOf(pageSize));
        ResponseEntity<GeneralEncountersResponse> responseEntity = avniHttpClient.get("/api/encounters", queryParams, GeneralEncountersResponse.class);
        return responseEntity.getBody();
    }

    public GeneralEncounter getGeneralEncounter(String id) {
        ResponseEntity<GeneralEncounter> responseEntity = avniHttpClient.get(String.format("/api/encounter/%s", id), GeneralEncounter.class);
        return responseEntity.getBody();
    }

    public GeneralEncounter get(HashMap<String, Object> concepts) {
        HashMap<String, String> queryParams = new HashMap<>();
        queryParams.put("concepts", ObjectJsonMapper.writeValueAsString(concepts));
        ResponseEntity<GeneralEncountersResponse> responseEntity = avniHttpClient.get("/api/encounters", queryParams, GeneralEncountersResponse.class);
        return pickAndExpectOne(responseEntity.getBody().getContent());
    }

    public GeneralEncounter get(String encounterType, Map<String, Object> concepts) {
        HashMap<String, String> queryParams = new HashMap<>();
        queryParams.put("concepts", ObjectJsonMapper.writeValueAsString(concepts));
        queryParams.put("encounterType", encounterType);
        ResponseEntity<GeneralEncountersResponse> responseEntity = avniHttpClient.get("/api/encounters", queryParams, GeneralEncountersResponse.class);
        return pickAndExpectOne(responseEntity.getBody().getContent());
    }

    public GeneralEncounter delete(String deletedEntity) {
        String json = null;
        HashMap<String, String> queryParams = new HashMap<>();
        ResponseEntity<GeneralEncounter> responseEntity = avniHttpClient.delete(String.format("/api/encounters/%s", deletedEntity), queryParams, json, GeneralEncounter.class);
        return responseEntity.getBody();
    }
}
