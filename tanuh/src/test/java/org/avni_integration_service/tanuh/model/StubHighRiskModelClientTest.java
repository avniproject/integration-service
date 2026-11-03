package org.avni_integration_service.tanuh.model;

import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfig;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.avni_integration_service.tanuh.config.TanuhConfig;
import org.avni_integration_service.tanuh.config.TanuhContextProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StubHighRiskModelClientTest {
    private final TanuhContextProvider contextProvider = new TanuhContextProvider();
    private final StubHighRiskModelClient client = new StubHighRiskModelClient(contextProvider);

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

    private static GeneralEncounter screening() {
        GeneralEncounter s = new GeneralEncounter();
        s.setUuid("a");
        return s;
    }

    @AfterEach
    public void tearDown() {
        TanuhContextProvider.clear();
    }

    @Test
    public void acceptModeTakesTheScreening() {
        settings("int_env", "local", "model_stub_mode", "accept");
        assertDoesNotThrow(() -> client.submit(screening(), List.of()));
    }

    @Test
    public void acceptIsTheDefault() {
        settings("int_env", "local");
        assertDoesNotThrow(() -> client.submit(screening(), List.of()));
    }

    @Test
    public void failModeFails() {
        settings("int_env", "local", "model_stub_mode", "fail");
        assertThrows(HighRiskModelException.class, () -> client.submit(screening(), List.of()));
    }

    @Test
    public void theStandInRefusesAnOrganisationSetToProductionInEveryMode() {
        for (String mode : List.of("accept", "fail")) {
            settings("int_env", "prod", "model_stub_mode", mode);
            HighRiskModelException e = assertThrows(HighRiskModelException.class, () -> client.submit(screening(), List.of()));
            assertTrue(e.getMessage().contains("production"), e.getMessage());
        }
    }
}
