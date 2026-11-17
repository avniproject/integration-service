package org.avni_integration_service.tanuh.service;

import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.avni_integration_service.tanuh.BaseTanuhSpringTest;
import org.avni_integration_service.tanuh.TanuhIntegrationService;
import org.avni_integration_service.tanuh.config.TanuhConfig;
import org.avni_integration_service.tanuh.config.TanuhContextProvider;
import org.avni_integration_service.tanuh.domain.TanuhConcepts.Concept;
import org.avni_integration_service.tanuh.repository.TanuhOcshrCaseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Against the local test database (avni_int_test), with the table built by its migration.
@SpringBootTest(classes = {TanuhIntegrationService.class})
public class DbSentScreeningsTest extends BaseTanuhSpringTest {
    private static final List<String> PHOTOS = List.of("s3://prod-user-media/tanuh_uat/s1-0.jpg", "s3://prod-user-media/tanuh_uat/s1-1.jpg");

    @Autowired
    private DbSentScreenings sentScreenings;
    @Autowired
    private TanuhOcshrCaseRepository repository;
    @Autowired
    private IntegrationSystemRepository integrationSystemRepository;
    @Autowired
    private TanuhContextProvider context;

    private IntegrationSystem one;
    private IntegrationSystem other;
    private String screening;

    @BeforeEach
    public void setUp() {
        one = system("sent_screenings_test_one");
        other = system("sent_screenings_test_other");
        screening = UUID.randomUUID().toString();
        context.set(new TanuhConfig(new IntegrationSystemConfigCollection(List.of()), one));
    }

    @AfterEach
    public void tearDown() {
        repository.findAll().stream()
                .filter(c -> c.getScreeningUuid().equals(screening))
                .forEach(repository::delete);
        integrationSystemRepository.delete(one);
        integrationSystemRepository.delete(other);
        TanuhContextProvider.clear();
    }

    // Test systems of their own, removed after each test, so no organisation's rows are touched.
    private IntegrationSystem system(String name) {
        IntegrationSystem existing = integrationSystemRepository.findBySystemTypeAndName(IntegrationSystem.IntegrationSystemType.tanuh, name);
        if (existing != null) return existing;
        IntegrationSystem system = new IntegrationSystem();
        ReflectionTestUtils.setField(system, "name", name);
        ReflectionTestUtils.setField(system, "systemType", IntegrationSystem.IntegrationSystemType.tanuh);
        return integrationSystemRepository.save(system);
    }

    @Test
    public void aScreeningNeverSentIsNotFound() {
        assertEquals(Optional.empty(), sentScreenings.find(screening));
    }

    @Test
    public void theFirstSendsPhotosAreKeptAndTheAnswerAddedLater() {
        sentScreenings.attempt(screening, PHOTOS);
        assertEquals(Optional.of(new SentScreenings.SentScreening(Optional.of(PHOTOS), Optional.empty())), sentScreenings.find(screening));

        sentScreenings.answer(screening, Concept.YES);
        assertEquals(Optional.of(new SentScreenings.SentScreening(Optional.of(PHOTOS), Optional.of(Concept.YES))), sentScreenings.find(screening));
    }

    @Test
    public void aLaterAttemptKeepsTheFirstPhotos() {
        sentScreenings.attempt(screening, PHOTOS);
        sentScreenings.attempt(screening, List.of("s3://prod-user-media/tanuh_uat/s1-9.jpg"));

        assertEquals(Optional.of(PHOTOS), sentScreenings.find(screening).flatMap(SentScreenings.SentScreening::imageUris));
    }

    @Test
    public void aScreeningMarkedNotSentWithoutASendIsKept() {
        sentScreenings.answer(screening, Concept.NO);

        assertEquals(Optional.of(new SentScreenings.SentScreening(Optional.empty(), Optional.of(Concept.NO))), sentScreenings.find(screening));
    }

    @Test
    public void eachOrganisationKeepsItsOwn() {
        sentScreenings.answer(screening, Concept.YES);
        context.set(new TanuhConfig(new IntegrationSystemConfigCollection(List.of()), other));

        assertEquals(Optional.empty(), sentScreenings.find(screening));
    }
}
