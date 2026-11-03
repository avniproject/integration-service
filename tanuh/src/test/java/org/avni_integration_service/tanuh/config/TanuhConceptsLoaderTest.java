package org.avni_integration_service.tanuh.config;

import org.avni_integration_service.integration_data.context.ContextIntegrationSystem;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.MappingMetaData;
import org.avni_integration_service.integration_data.domain.MappingType;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.avni_integration_service.integration_data.repository.MappingMetaDataRepository;
import org.avni_integration_service.tanuh.domain.TanuhConcepts;
import org.avni_integration_service.tanuh.domain.TanuhConceptsFixture;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TanuhConceptsLoaderTest {
    private final MappingMetaDataRepository repository = mock(MappingMetaDataRepository.class);
    private final IntegrationSystemRepository integrationSystemRepository = mock(IntegrationSystemRepository.class);
    private final TanuhConceptsLoader loader = new TanuhConceptsLoader(repository, integrationSystemRepository);
    private final IntegrationSystem system = new IntegrationSystem();

    public TanuhConceptsLoaderTest() {
        system.setId(15);
        ReflectionTestUtils.setField(system, "name", "tanuh_uat");
        when(integrationSystemRepository.findById(15)).thenReturn(Optional.of(system));
    }

    private static MappingMetaData row(String type, String key, String name, boolean voided) {
        MappingType mappingType = new MappingType();
        mappingType.setName(type);
        MappingMetaData row = new MappingMetaData();
        row.setMappingType(mappingType);
        row.setIntSystemValue(key);
        row.setAvniValue(name);
        row.setVoided(voided);
        return row;
    }

    private void organisationHas(List<MappingMetaData> rows) {
        when(repository.findAllByMappingGroupNameAndIntegrationSystem(TanuhMappingDbConstants.MappingGroup_OralScreening, system)).thenReturn(rows);
    }

    private static List<MappingMetaData> bundleRows() {
        List<MappingMetaData> rows = new ArrayList<>();
        for (TanuhConcepts.Mapping m : TanuhConceptsFixture.bundleRows())
            rows.add(row(TanuhMappingDbConstants.MappingType_Concept, m.key(), m.name(), false));
        return rows;
    }

    @Test
    public void theOrganisationsLiveConceptRowsAreTheNames() {
        List<MappingMetaData> rows = bundleRows();
        rows.add(row(TanuhMappingDbConstants.MappingType_Concept, "qc_pass", "Old pass", true));
        rows.add(row("Something else", "qc_pass", "Other type", false));
        organisationHas(rows);

        TanuhConcepts concepts = loader.load(new ContextIntegrationSystem(system));

        assertEquals("QC Pass", concepts.name(TanuhConcepts.Concept.QC_PASS));
    }

    @Test
    public void anOrganisationWithoutRowsIsToldWhatIsMissing() {
        organisationHas(List.of());

        IllegalStateException e = assertThrows(IllegalStateException.class, () -> loader.load(new ContextIntegrationSystem(system)));

        assertTrue(e.getMessage().startsWith("[tanuh_uat] Tanuh mapping: no row for yes"), e.getMessage());
    }
}
