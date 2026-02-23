package org.avni_integration_service.bahmni.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import org.avni_integration_service.bahmni.client.OpenMRSWebClient;
import org.avni_integration_service.bahmni.contract.*;
import org.avni_integration_service.avni.MultipleResultsFoundException;
import org.avni_integration_service.util.ObjectJsonMapper;
import org.ict4h.atomfeed.client.domain.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OpenMRSEncounterRepository extends BaseOpenMRSRepository {
    @Autowired
    public OpenMRSEncounterRepository(OpenMRSWebClient openMRSWebClient) {
        super(openMRSWebClient);
    }

    public OpenMRSFullEncounter getEncounterByUuid(String uuid) {
        String json = openMRSWebClient.get(String.format("%s?v=full", getSingleResourcePath("encounter", uuid)));
        return ObjectJsonMapper.readValue(json, OpenMRSFullEncounter.class);
    }

    public OpenMRSFullEncounter getEncounterByPatientAndObservation(String patientUuid, String conceptUuid, String value) {
        String json = openMRSWebClient.get(URI.create(String.format("%s?patient=%s&obsConcept=%s&obsValues=%s", getResourcePath("encounter"), patientUuid, conceptUuid, encode(value))));
        SearchResults<OpenMRSUuidHolder> searchResults = ObjectJsonMapper.readValue(json, new TypeReference<SearchResults<OpenMRSUuidHolder>>() {
        });
        OpenMRSUuidHolder encounterReference = pickAndExpectOne(searchResults, String.format("More than one entity found with params %s %s %s", patientUuid, conceptUuid, value));
        return encounterReference == null ? null : getEncounterByUuid(encounterReference.getUuid());
    }

    public OpenMRSFullEncounter getEncounterByPatientAndObservationAndEncType(String patientUuid, String conceptUuid, String value, String encounterTypeUuid) {
        String json = openMRSWebClient.get(URI.create(String.format("%s?patient=%s&obsConcept=%s&obsValues=%s&v=full", getResourcePath("encounter"), patientUuid, conceptUuid, encode(value))));
        SearchResults<OpenMRSFullEncounter> searchResults = ObjectJsonMapper.readValue(json, new TypeReference<SearchResults<OpenMRSFullEncounter>>() {
        });
        List<OpenMRSFullEncounter> filteredByEncType = searchResults.getResults().stream().filter(e -> e.getEncounterType().getUuid().equals(encounterTypeUuid)).collect(Collectors.toList());
        if (filteredByEncType.size() == 0) return null;
        if (filteredByEncType.size() == 1) return filteredByEncType.get(0);
        throw new MultipleResultsFoundException(String.format("More than one entity found with params %s %s %s %s", patientUuid, conceptUuid, value, encounterTypeUuid));
    }

    public List<OpenMRSFullEncounter> getEncounterByPatientAndEncType(String patientUuid, String encounterTypeUuid) {
        String json = openMRSWebClient.get(URI.create(String.format("%s?patient=%s&encounterType=%s&v=full", getResourcePath("encounter"), patientUuid, encounterTypeUuid)));
        SearchResults<OpenMRSFullEncounter> searchResults = ObjectJsonMapper.readValue(json, new TypeReference<SearchResults<OpenMRSFullEncounter>>() {
        });
        if (searchResults.getResults().size() == 0) return new ArrayList<>();
        return searchResults.getResults();
    }

    public OpenMRSFullEncounter createEncounter(OpenMRSEncounter encounter) {
        System.err.println("\n=== OpenMRSEncounterRepository.createEncounter ===");
        System.err.println("Encounter Type UUID: " + encounter.getEncounterType());
        System.err.println("Patient UUID: " + encounter.getPatient());
        System.err.println("Visit UUID: " + encounter.getVisit());
        System.err.println("Location UUID: " + encounter.getLocation());
        System.err.println("Observations count: " + (encounter.getObservations() != null ? encounter.getObservations().size() : 0));

        String json = ObjectJsonMapper.writeValueAsString(encounter);
        System.err.println("Encounter JSON (first 600 chars): " + json.substring(0, Math.min(600, json.length())) + (json.length() > 600 ? "\n...TRUNCATED..." : ""));

        System.err.println("POSTing to Bahmni encounter endpoint...");
        String outputJson = openMRSWebClient.post(getResourcePath("encounter"), json);
        System.err.println("✓ Bahmni response (first 300 chars): " + outputJson.substring(0, Math.min(300, outputJson.length())) + (outputJson.length() > 300 ? "\n...TRUNCATED..." : ""));
        return ObjectJsonMapper.readValue(outputJson, OpenMRSFullEncounter.class);
    }

    public OpenMRSFullEncounter updateEncounter(OpenMRSEncounter encounter) {
        String json = ObjectJsonMapper.writeValueAsString(encounter);
        String outputJson = openMRSWebClient.post(getResourcePath(String.format("encounter/%s", encounter.getUuid())), json);
        return ObjectJsonMapper.readValue(outputJson, OpenMRSFullEncounter.class);
    }

    public void deleteEncounter(OpenMRSBaseEncounter encounter) {
        openMRSWebClient.delete(URI.create(String.format("%s/%s?purge=true", getResourcePath("encounter"), encounter.getUuid())));
    }

    public OpenMRSFullEncounter getEncounter(Event event) {
        String json = getUnderlyingResourceJson(event);
        return ObjectJsonMapper.readValue(json, OpenMRSFullEncounter.class);
    }

    public OpenMRSDefaultEncounter getDefaultEncounter(String uuid) {
        String json = openMRSWebClient.get(URI.create(String.format("%s/%s/encounter/%s", urlPrefix, BaseOpenMRSRepository.OPENMRS_BASE_PATH, uuid)));
        return ObjectJsonMapper.readValue(json, OpenMRSDefaultEncounter.class);
    }

    public void voidEncounter(OpenMRSFullEncounter existingEncounter) {
        openMRSWebClient.delete(URI.create(String.format("%s/%s", getResourcePath("encounter"), existingEncounter.getUuid())));
    }
}
