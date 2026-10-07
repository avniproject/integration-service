package org.avni_integration_service.tanuh.model;

import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfig;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.avni_integration_service.tanuh.config.TanuhConfig;
import org.avni_integration_service.tanuh.config.TanuhContextProvider;
import org.avni_integration_service.tanuh.domain.ModelResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StubHighRiskModelClientTest {
    private final TanuhContextProvider contextProvider = new TanuhContextProvider();
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-07T09:15:00.123Z"), ZoneOffset.UTC);
    private final StubHighRiskModelClient client = new StubHighRiskModelClient(contextProvider, clock);

    private void settings(String... keyValues) {
        List<IntegrationSystemConfig> configs = new ArrayList<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            IntegrationSystemConfig c = new IntegrationSystemConfig();
            c.setKey(keyValues[i]);
            c.setValue(keyValues[i + 1]);
            configs.add(c);
        }
        IntegrationSystem system = new IntegrationSystem();
        system.setId(1);
        ReflectionTestUtils.setField(system, "name", "tanuh_uat_local");
        contextProvider.set(new TanuhConfig(new IntegrationSystemConfigCollection(configs), system));
    }

    private static GeneralEncounter screening(String uuid) {
        GeneralEncounter s = new GeneralEncounter();
        s.setUuid(uuid);
        return s;
    }

    @AfterEach
    public void tearDown() {
        TanuhContextProvider.clear();
    }

    @Test
    public void fixedModeAnswersTheFixedResult() throws Exception {
        settings("int_env", "local", "model_stub_mode", "fixed", "model_stub_fixed_result", "Low Risk");
        ModelResult r = client.score(screening("a"), List.of());
        assertEquals(ModelResult.Result.LOW_RISK, r.result());
        assertEquals("stub", r.version());
        assertEquals(Instant.parse("2026-10-07T09:15:00.123Z"), r.runTime());
    }

    @Test
    public void fixedModeWithoutAValidResultFailsTheScreening() {
        settings("int_env", "local", "model_stub_mode", "fixed");
        assertThrows(HighRiskModelException.class, () -> client.score(screening("a"), List.of()));
    }

    @Test
    public void byEncounterGivesTheSameScreeningTheSameAnswerAndSpreadsAcrossAllThree() throws Exception {
        settings("int_env", "local", "model_stub_mode", "by_encounter");
        // Hand-computed: String.hashCode() of each id, floorMod 3 (1551938949 -> 0, 1503488842 -> 1, -739572304 -> 2).
        // These are Tanuh UAT Local's S03, S01 and S02, so the same values predict the live run.
        assertEquals(ModelResult.Result.HIGH_RISK, client.score(screening("c6a05cc5-6d2a-4fbd-8ac3-c95abd542bb4"), List.of()).result());
        assertEquals(ModelResult.Result.LOW_RISK, client.score(screening("a4312f00-2cd9-41a5-8e38-d6520a151009"), List.of()).result());
        assertEquals(ModelResult.Result.NOT_SUSPICIOUS, client.score(screening("a8eacda9-3fd9-4585-8c11-42eaaa29ca60"), List.of()).result());
        assertEquals(ModelResult.Result.LOW_RISK, client.score(screening("a4312f00-2cd9-41a5-8e38-d6520a151009"), List.of()).result());
    }

    @Test
    public void failModeFails() {
        settings("int_env", "local", "model_stub_mode", "fail");
        assertThrows(HighRiskModelException.class, () -> client.score(screening("a"), List.of()));
    }

    @Test
    public void theStandInRefusesAnOrganisationSetToProductionInEveryMode() {
        for (String mode : List.of("fixed", "by_encounter", "fail")) {
            settings("int_env", "prod", "model_stub_mode", mode, "model_stub_fixed_result", "High Risk");
            HighRiskModelException e = assertThrows(HighRiskModelException.class, () -> client.score(screening("a"), List.of()));
            assertTrue(e.getMessage().contains("production"), e.getMessage());
        }
    }
}
