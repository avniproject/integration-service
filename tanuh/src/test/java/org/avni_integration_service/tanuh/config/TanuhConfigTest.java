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
    public void theHighRiskServicesUserIsTheTrimmedSettingOrNull() {
        assertNull(configWith().getHighRiskAvniUser());
        assertNull(configWith("high_risk_avni_user", "  ").getHighRiskAvniUser());
        assertEquals("high-risk@tanuh_uat", configWith("high_risk_avni_user", " high-risk@tanuh_uat ").getHighRiskAvniUser());
    }

    @Test
    public void theRulesThatFailQcAreTheListedIdsOrTheBlockingChecks() {
        assertEquals(Set.of("ST-001", "QC-030"), configWith().getQcFailRules());
        assertEquals(Set.of("ST-001", "QC-030"), configWith("qc_fail_rules", "").getQcFailRules());
        assertEquals(Set.of("ST-001", "QC-030"), configWith("qc_fail_rules", " ").getQcFailRules(), "no id in it");
        assertEquals(Set.of("ST-001", "QC-030"), configWith("qc_fail_rules", " , ,").getQcFailRules(), "no id in it");
        assertEquals(Set.of("SL-001", "samvit:ORAL-003"), configWith("qc_fail_rules", " SL-001 ,, samvit:ORAL-003 ").getQcFailRules());
    }

    // Off only when the setting says false, so a missing or mistyped row keeps the check and its failures in sight.
    @Test
    public void theDataQualityCheckIsOnUnlessTheSettingSaysFalse() {
        assertTrue(configWith().isDataQualityCheckOn());
        assertTrue(configWith("dqs_enabled", "true").isDataQualityCheckOn());
        assertTrue(configWith("dqs_enabled", "flase").isDataQualityCheckOn(), "a typo keeps it on");
        assertFalse(configWith("dqs_enabled", "false").isDataQualityCheckOn());
        assertFalse(configWith("dqs_enabled", " FALSE ").isDataQualityCheckOn());
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

    @Test
    public void theGoogleSignInSettingsAreTrimmedWithDefaults() {
        TanuhConfig none = configWith();
        assertNull(none.getOcshrCredentialConfig());
        assertEquals("sa-ocshr-avni@proj-model-hub.iam.gserviceaccount.com", none.getOcshrServiceAccount());
        assertEquals("medgemma-intake", none.getOcshrAudience());
        assertNull(configWith("ocshr_credential_config", "  ").getOcshrCredentialConfig());
        TanuhConfig set = configWith("ocshr_credential_config", " {} ", "ocshr_service_account", " a@b.c ", "ocshr_audience", " aud ");
        assertEquals("{}", set.getOcshrCredentialConfig());
        assertEquals("a@b.c", set.getOcshrServiceAccount());
        assertEquals("aud", set.getOcshrAudience());
    }

    @Test
    public void theModelServiceAddressIsTrimmedWithoutTrailingSlashesAndNullWhenUnset() {
        assertNull(configWith().getOcshrApiUrl());
        assertNull(configWith("ocshr_api_url", "  ").getOcshrApiUrl());
        assertEquals("https://ocshr.example.org", configWith("ocshr_api_url", " https://ocshr.example.org// ").getOcshrApiUrl());
    }

    @Test
    public void theOrganisationIdIsAPositiveWholeNumberOrNull() {
        assertNull(configWith().getAvniOrganisationId());
        assertNull(configWith("avni_organisation_id", "  ").getAvniOrganisationId());
        assertNull(configWith("avni_organisation_id", "tanuh_uat").getAvniOrganisationId());
        assertNull(configWith("avni_organisation_id", "0").getAvniOrganisationId());
        assertNull(configWith("avni_organisation_id", "-1071").getAvniOrganisationId());
        assertNull(configWith("avni_organisation_id", "1071.0").getAvniOrganisationId());
        assertEquals(1071L, configWith("avni_organisation_id", " 1071 ").getAvniOrganisationId());
    }
}
