package org.avni_integration_service.tanuh.config;

import org.apache.log4j.Logger;
import org.avni_integration_service.integration_data.context.ContextIntegrationSystem;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class TanuhConfig {
    private static final Logger logger = Logger.getLogger(TanuhConfig.class);

    public static final String DEFAULT_STUB_MODE = "accept";
    private static final List<String> STUB_MODES = List.of("accept", "fail");
    // The checks sheet's blocking checks among those Samvit runs on an oral screening: no visit time or one in the
    // future, no usable image, unusable images.
    public static final String DEFAULT_QC_FAIL_RULES = "ST-001,SM-003,QC-030";

    private final IntegrationSystemConfigCollection integrationSystemConfigCollection;
    private final ContextIntegrationSystem integrationSystem;
    private final String stubMode;
    private final Set<String> qcFailRules;

    public TanuhConfig(IntegrationSystemConfigCollection integrationSystemConfigCollection, IntegrationSystem integrationSystem) {
        this.integrationSystemConfigCollection = integrationSystemConfigCollection;
        this.integrationSystem = new ContextIntegrationSystem(integrationSystem);
        this.stubMode = readStubMode();
        this.qcFailRules = readQcFailRules();
    }

    private String getStringConfigValue(String key, String defaultValue) {
        String configValue = integrationSystemConfigCollection.getConfigValue(key);
        return StringUtils.hasLength(configValue) ? configValue : defaultValue;
    }

    private String readStubMode() {
        String value = getStringConfigValue("model_stub_mode", null);
        if (!StringUtils.hasText(value)) return DEFAULT_STUB_MODE;
        if (STUB_MODES.contains(value.trim())) return value.trim();
        logger.warn(String.format("[%s] model_stub_mode '%s' is not one of %s, using %s", integrationSystem.getName(), value, STUB_MODES, DEFAULT_STUB_MODE));
        return DEFAULT_STUB_MODE;
    }

    private Set<String> readQcFailRules() {
        return Arrays.stream(getStringConfigValue("qc_fail_rules", DEFAULT_QC_FAIL_RULES).split(","))
                .map(String::trim).filter(StringUtils::hasText).collect(Collectors.toUnmodifiableSet());
    }

    public String getApiUrl() {
        return getStringConfigValue("avni_api_url", "https://app.avniproject.org");
    }

    public String getAvniImplUser() {
        return getStringConfigValue("avni_user", "dummy");
    }

    public String getImplPassword() {
        return getStringConfigValue("avni_password", "dummy");
    }

    public boolean getAuthEnabled() {
        return Boolean.parseBoolean(getStringConfigValue("avni_auth_enabled", "true"));
    }

    public String getEnvironment() {
        return getStringConfigValue("int_env", null);
    }

    public String getStubMode() {
        return stubMode;
    }

    public ContextIntegrationSystem getIntegrationSystem() {
        return integrationSystem;
    }

    public String getMainJobHealthCheckSlug() {
        return integrationSystem.getName().toLowerCase(Locale.ROOT);
    }

    public String getErrorJobHealthCheckSlug() {
        return getMainJobHealthCheckSlug() + "-error";
    }

    // The data quality service's address, with no trailing slash; null when the organisation has none configured.
    public String getDqsApiUrl() {
        String value = getStringConfigValue("dqs_api_url", null);
        if (!StringUtils.hasText(value)) return null;
        value = value.trim();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }

    public String getDqsTenant() {
        return getStringConfigValue("dqs_tenant", "samvit").trim();
    }

    // The data quality service's rules whose failure makes a screening QC Fail. A failed rule not in this set is
    // logged and does not fail it.
    public Set<String> getQcFailRules() {
        return qcFailRules;
    }

    // The Avni user the high-risk model service writes its result as; null when not set. A screening it changed last
    // is not sent again, or each result it wrote would send the screening back to it.
    public String getHighRiskAvniUser() {
        String value = getStringConfigValue("high_risk_avni_user", null);
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
