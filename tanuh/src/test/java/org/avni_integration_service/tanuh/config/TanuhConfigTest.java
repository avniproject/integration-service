package org.avni_integration_service.tanuh.config;

import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfig;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TanuhConfigTest {

    private TanuhConfig configWith(String... keyValues) {
        List<IntegrationSystemConfig> configs = new ArrayList<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            IntegrationSystemConfig c = new IntegrationSystemConfig();
            c.setKey(keyValues[i]);
            c.setValue(keyValues[i + 1]);
            configs.add(c);
        }
        IntegrationSystem integrationSystem = new IntegrationSystem();
        integrationSystem.setId(1);
        ReflectionTestUtils.setField(integrationSystem, "name", "Tanuh_UAT_Local");
        return new TanuhConfig(new IntegrationSystemConfigCollection(configs), integrationSystem);
    }

    @Test
    public void safetySampleRateDefaultsWhenMissingOrBlank() {
        assertEquals(0.05, configWith().getSafetySampleRate());
        assertEquals(0.05, configWith("safety_sample_rate", "").getSafetySampleRate());
        assertEquals(0.05, configWith("safety_sample_rate", "   ").getSafetySampleRate());
    }

    @Test
    public void safetySampleRateReadsAValueFromZeroToOne() {
        assertEquals(0.1, configWith("safety_sample_rate", "0.1").getSafetySampleRate());
        assertEquals(0.2, configWith("safety_sample_rate", " 0.2 ").getSafetySampleRate());
        assertEquals(0.0, configWith("safety_sample_rate", "0").getSafetySampleRate());
        assertEquals(1.0, configWith("safety_sample_rate", "1").getSafetySampleRate());
    }

    @Test
    public void safetySampleRateDefaultsWhenNotANumberOrOutOfRange() {
        for (String bad : List.of("abc", "1.5", "-0.1", "NaN", "Infinity", "-Infinity")) {
            assertEquals(0.05, configWith("safety_sample_rate", bad).getSafetySampleRate(), bad);
        }
    }

    @Test
    public void stubModeIsOneOfTheThreeOrTheDefault() {
        assertEquals("by_encounter", configWith().getStubMode());
        assertEquals("fixed", configWith("model_stub_mode", "fixed").getStubMode());
        assertEquals("fail", configWith("model_stub_mode", " fail ").getStubMode());
        assertEquals("by_encounter", configWith("model_stub_mode", "by_encounter").getStubMode());
        assertEquals("by_encounter", configWith("model_stub_mode", "Fixed").getStubMode());
        assertEquals("by_encounter", configWith("model_stub_mode", "random").getStubMode());
    }

    @Test
    public void stubFixedResultIsOneOfTheModelsAnswersOrNull() {
        assertNull(configWith().getStubFixedResult());
        assertEquals("High Risk", configWith("model_stub_fixed_result", "High Risk").getStubFixedResult());
        assertEquals("Low Risk", configWith("model_stub_fixed_result", " Low Risk ").getStubFixedResult());
        assertEquals("Non Suspicious", configWith("model_stub_fixed_result", "Non Suspicious").getStubFixedResult());
        assertNull(configWith("model_stub_fixed_result", "Medium").getStubFixedResult());
    }

    @Test
    public void connectionSettingsComeFromTheRows() {
        TanuhConfig config = configWith("avni_api_url", "http://localhost:8021", "avni_user", "integration@tanuh_uat_local",
                "avni_password", "secret", "avni_auth_enabled", "false", "int_env", "local");
        assertEquals("http://localhost:8021", config.getApiUrl());
        assertEquals("integration@tanuh_uat_local", config.getAvniImplUser());
        assertEquals("secret", config.getImplPassword());
        assertFalse(config.getAuthEnabled());
        assertEquals("local", config.getEnvironment());
    }

    @Test
    public void connectionSettingsDefaultLikeWati() {
        TanuhConfig config = configWith();
        assertEquals("https://app.avniproject.org", config.getApiUrl());
        assertTrue(config.getAuthEnabled());
        assertNull(config.getEnvironment());
    }

    @Test
    public void healthCheckSlugsComeFromTheRowName() {
        TanuhConfig config = configWith();
        assertEquals("tanuh_uat_local", config.getMainJobHealthCheckSlug());
        assertEquals("tanuh_uat_local-error", config.getErrorJobHealthCheckSlug());
        assertEquals("Tanuh_UAT_Local", config.getIntegrationSystem().getName());
    }
}
