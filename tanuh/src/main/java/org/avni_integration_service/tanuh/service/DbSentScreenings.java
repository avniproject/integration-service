package org.avni_integration_service.tanuh.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.avni_integration_service.tanuh.config.TanuhContextProvider;
import org.avni_integration_service.tanuh.domain.TanuhConcepts.Concept;
import org.avni_integration_service.tanuh.domain.TanuhOcshrCase;
import org.avni_integration_service.tanuh.repository.TanuhOcshrCaseRepository;
import org.avni_integration_service.util.ObjectJsonMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

// SentScreenings in the integration service's own database, table tanuh_ocshr_case (V2_4_10), one row per screening of
// the organisation the job is running for.
@Component
public class DbSentScreenings implements SentScreenings {
    private final TanuhOcshrCaseRepository repository;
    private final IntegrationSystemRepository integrationSystemRepository;
    private final TanuhContextProvider tanuhContextProvider;

    public DbSentScreenings(TanuhOcshrCaseRepository repository, IntegrationSystemRepository integrationSystemRepository,
                            TanuhContextProvider tanuhContextProvider) {
        this.repository = repository;
        this.integrationSystemRepository = integrationSystemRepository;
        this.tanuhContextProvider = tanuhContextProvider;
    }

    @Override
    public Optional<SentScreening> find(String screeningUuid) {
        return Optional.ofNullable(row(screeningUuid)).map(row -> new SentScreening(
                Optional.ofNullable(row.getImageUris()).map(DbSentScreenings::readUris),
                Optional.ofNullable(row.getCallMade()).map(Concept::valueOf)));
    }

    @Override
    public void attempt(String screeningUuid, List<String> imageUris) {
        if (row(screeningUuid) != null) return;
        TanuhOcshrCase row = newRow(screeningUuid);
        row.setImageUris(ObjectJsonMapper.writeValueAsString(imageUris));
        repository.save(row);
    }

    @Override
    public void answer(String screeningUuid, Concept callMade) {
        TanuhOcshrCase row = Optional.ofNullable(row(screeningUuid)).orElseGet(() -> newRow(screeningUuid));
        row.setCallMade(callMade.name());
        repository.save(row);
    }

    private TanuhOcshrCase row(String screeningUuid) {
        return repository.findByIntegrationSystem_IdAndScreeningUuid(tanuhContextProvider.get().getIntegrationSystem().getId(), screeningUuid);
    }

    private TanuhOcshrCase newRow(String screeningUuid) {
        TanuhOcshrCase row = new TanuhOcshrCase();
        row.setIntegrationSystem(integrationSystemRepository.findEntity(tanuhContextProvider.get().getIntegrationSystem().getId()));
        row.setScreeningUuid(screeningUuid);
        return row;
    }

    private static List<String> readUris(String json) {
        try {
            return ObjectJsonMapper.objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("tanuh_ocshr_case.image_uris is not a list of photo locations", e);
        }
    }
}
