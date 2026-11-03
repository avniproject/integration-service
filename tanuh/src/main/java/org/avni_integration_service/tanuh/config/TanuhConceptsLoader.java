package org.avni_integration_service.tanuh.config;

import org.avni_integration_service.integration_data.context.ContextIntegrationSystem;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.avni_integration_service.integration_data.repository.MappingMetaDataRepository;
import org.avni_integration_service.tanuh.domain.TanuhConcepts;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.avni_integration_service.tanuh.config.TanuhMappingDbConstants.MappingGroup_OralScreening;
import static org.avni_integration_service.tanuh.config.TanuhMappingDbConstants.MappingType_Concept;

// Reads the organisation's Tanuh names from mapping_metadata. Each job does this as its run starts, so an edit made in
// the admin app applies from the next run. Rows the job cannot trust fail the run with every fault named. The
// transaction keeps the session open while each row's lazy mapping type is read.
@Component
public class TanuhConceptsLoader {
    private final MappingMetaDataRepository mappingMetaDataRepository;
    private final IntegrationSystemRepository integrationSystemRepository;

    public TanuhConceptsLoader(MappingMetaDataRepository mappingMetaDataRepository, IntegrationSystemRepository integrationSystemRepository) {
        this.mappingMetaDataRepository = mappingMetaDataRepository;
        this.integrationSystemRepository = integrationSystemRepository;
    }

    @Transactional(readOnly = true)
    public TanuhConcepts load(ContextIntegrationSystem contextIntegrationSystem) {
        IntegrationSystem integrationSystem = integrationSystemRepository.findById(contextIntegrationSystem.getId()).orElseThrow();
        List<TanuhConcepts.Mapping> rows = mappingMetaDataRepository.findAllByMappingGroupNameAndIntegrationSystem(MappingGroup_OralScreening, integrationSystem)
                .stream()
                .filter(row -> !row.isVoided() && MappingType_Concept.equals(row.getMappingType().getName()))
                .map(row -> new TanuhConcepts.Mapping(row.getIntSystemValue(), row.getAvniValue()))
                .toList();
        try {
            return TanuhConcepts.from(rows);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(String.format("[%s] %s", contextIntegrationSystem.getName(), e.getMessage()), e);
        }
    }
}
