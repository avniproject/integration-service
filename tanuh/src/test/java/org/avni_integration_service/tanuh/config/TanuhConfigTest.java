package org.avni_integration_service.tanuh.config;

import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfig;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    public void stubModeIsAcceptOrFailOrTheDefault() {
        assertEquals("accept", configWith().getStubMode());
        assertEquals("fail", configWith("model_stub_mode", " fail ").getStubMode());
        assertEquals("accept", configWith("model_stub_mode", "accept").getStubMode());
        assertEquals("accept", configWith("model_stub_mode", "by_encounter").getStubMode());
        assertEquals("accept", configWith("model_stub_mode", "Fail").getStubMode());
    }

    @Test
    public void theHighRiskServicesUserIsTheTrimmedSettingOrNull() {
        assertNull(configWith().getHighRiskAvniUser());
        assertNull(configWith("high_risk_avni_user", "  ").getHighRiskAvniUser());
        assertEquals("high-risk@tanuh_uat", configWith("high_risk_avni_user", " high-risk@tanuh_uat ").getHighRiskAvniUser());
    }

    @Test
    public void theRulesThatFailQcAreTheListedIdsOrTheBlockingChecks() {
        assertEquals(Set.of("ST-001", "SM-003", "QC-030"), configWith().getQcFailRules());
        assertEquals(Set.of("ST-001", "SM-003", "QC-030"), configWith("qc_fail_rules", "").getQcFailRules());
        assertEquals(Set.of("SL-001", "samvit:ORAL-003"), configWith("qc_fail_rules", " SL-001 ,, samvit:ORAL-003 ").getQcFailRules());
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

    @Test
    public void dqsSettingsComeFromTheRowsWithTheAddressTrimmed() {
        TanuhConfig config = configWith("dqs_api_url", " https://dqs.example.org/ ", "dqs_tenant", "samvit");
        assertEquals("https://dqs.example.org", config.getDqsApiUrl());
        assertEquals("samvit", config.getDqsTenant());
    }

    @Test
    public void dqsHasNoAddressUntilOneIsSet() {
        TanuhConfig config = configWith("dqs_api_url", "  ");
        assertNull(config.getDqsApiUrl());
        assertEquals("samvit", config.getDqsTenant());
    }
}
