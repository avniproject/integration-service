package org.avni_integration_service.tanuh.config;

import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TanuhContextProviderTest {
    private final TanuhContextProvider provider = new TanuhContextProvider();

    @AfterEach
    public void tearDown() {
        TanuhContextProvider.clear();
    }

    @Test
    public void getFailsUntilAConfigIsSetAndAfterItIsCleared() {
        assertThrows(IllegalStateException.class, provider::get);
        IntegrationSystem system = new IntegrationSystem();
        system.setId(1);
        TanuhConfig config = new TanuhConfig(new IntegrationSystemConfigCollection(List.of()), system);

        provider.set(config);
        assertSame(config, provider.get());

        TanuhContextProvider.clear();
        assertThrows(IllegalStateException.class, provider::get);
    }
}
