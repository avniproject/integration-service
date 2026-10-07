package org.avni_integration_service.tanuh.service;

import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfig;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.avni_integration_service.tanuh.config.TanuhConfig;
import org.avni_integration_service.tanuh.config.TanuhContextProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class SamplerTest {
    private final TanuhContextProvider contextProvider = new TanuhContextProvider();

    private void rate(String rate) {
        IntegrationSystemConfig c = new IntegrationSystemConfig();
        c.setKey("safety_sample_rate");
        c.setValue(rate);
        IntegrationSystem system = new IntegrationSystem();
        system.setId(1);
        ReflectionTestUtils.setField(system, "name", "tanuh_uat_local");
        contextProvider.set(new TanuhConfig(new IntegrationSystemConfigCollection(List.of(c)), system));
    }

    @AfterEach
    public void tearDown() {
        TanuhContextProvider.clear();
    }

    @Test
    public void tenThousandSeededDrawsPickBetweenFourAndAHalfAndFiveAndAHalfInAHundred() {
        rate("0.05");
        Sampler sampler = new Sampler(contextProvider, new Random(20261007L));
        int picked = 0;
        for (int i = 0; i < 10_000; i++) if (sampler.draw("s" + i)) picked++;
        assertTrue(picked >= 450 && picked <= 550, "picked " + picked);
    }

    @Test
    public void theRateComesFromTheRunningOrganisation() {
        rate("0");
        Sampler sampler = new Sampler(contextProvider, new Random(1));
        for (int i = 0; i < 1000; i++) assertFalse(sampler.draw("s" + i));
        rate("1");
        for (int i = 0; i < 1000; i++) assertTrue(sampler.draw("s" + i));
    }
}
