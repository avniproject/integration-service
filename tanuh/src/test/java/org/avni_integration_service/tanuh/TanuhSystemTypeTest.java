package org.avni_integration_service.tanuh;

import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TanuhSystemTypeTest {
    @Test
    public void tanuhRowsAreStoredWithTheSystemTypeTanuh() {
        assertEquals("tanuh", IntegrationSystem.IntegrationSystemType.valueOf("tanuh").name());
    }
}
