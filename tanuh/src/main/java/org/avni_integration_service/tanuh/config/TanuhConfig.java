package org.avni_integration_service.tanuh.config;

import org.avni_integration_service.integration_data.context.ContextIntegrationSystem;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class TanuhConfig {
    // The checks sheet's blocking checks among those Samvit runs on an oral screening: no visit time or one in the
    // future, no usable image, unusable images.
    public static final String DEFAULT_QC_FAIL_RULES = "ST-001,SM-003,QC-030";

    private final IntegrationSystemConfigCollection integrationSystemConfigCollection;
    private final ContextIntegrationSystem integrationSystem;
    private final Set<String> qcFailRules;

    public TanuhConfig(IntegrationSystemConfigCollection integrationSystemConfigCollection, IntegrationSystem integrationSystem) {
        this.integrationSystemConfigCollection = integrationSystemConfigCollection;
        this.integrationSystem = new ContextIntegrationSystem(integrationSystem);
        this.qcFailRules = readQcFailRules();
    }

    private String getStringConfigValue(String key, String defaultValue) {
        String configValue = integrationSystemConfigCollection.getConfigValue(key);
        return StringUtils.hasLength(configValue) ? configValue : defaultValue;
    }

    // A value with no rule id in it, such as " " or ",", would fail no screening, so it means the default too.
    private Set<String> readQcFailRules() {
        Set<String> rules = ruleIds(getStringConfigValue("qc_fail_rules", DEFAULT_QC_FAIL_RULES));
        return rules.isEmpty() ? ruleIds(DEFAULT_QC_FAIL_RULES) : rules;
    }

    private static Set<String> ruleIds(String value) {
        return Arrays.stream(value.split(",")).map(String::trim).filter(StringUtils::hasText).collect(Collectors.toUnmodifiableSet());
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

    public ContextIntegrationSystem getIntegrationSystem() {
        return integrationSystem;
    }

    public String getMainJobHealthCheckSlug() {
        return integrationSystem.getName().toLowerCase(Locale.ROOT);
    }

    public String getErrorJobHealthCheckSlug() {
        return getMainJobHealthCheckSlug() + "-error";
    }

    // The model service's address, with no trailing slash; null when the organisation has none configured.
    public String getOcshrApiUrl() {
        String value = getStringConfigValue("ocshr_api_url", null);
        if (!StringUtils.hasText(value)) return null;
        value = value.trim();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }

    // Whether each screening goes to the data quality service, and gets its QC verdict, before it is sent to Tanuh's
    // model. Switched off for now (Adam, 17 Nov 2026) by dqs_enabled false. Only false turns it off, so a missing or
    // mistyped row keeps the check, whose failures show, rather than skipping it without a sign.
    public boolean isDataQualityCheckOn() {
        return !"false".equalsIgnoreCase(getStringConfigValue("dqs_enabled", "true").trim());
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

    // Tanuh's credential configuration file (AWS workload identity federation), kept as the JSON text; null when not
    // set, which holds screenings back until it is.
    public String getOcshrCredentialConfig() {
        String value = getStringConfigValue("ocshr_credential_config", null);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    // The service account Tanuh's model service accepts calls from.
    public String getOcshrServiceAccount() {
        String value = getStringConfigValue("ocshr_service_account", null);
        return StringUtils.hasText(value) ? value.trim() : "sa-ocshr-avni@proj-model-hub.iam.gserviceaccount.com";
    }

    // The audience Tanuh's model service expects in the ID token.
    public String getOcshrAudience() {
        String value = getStringConfigValue("ocshr_audience", null);
        return StringUtils.hasText(value) ? value.trim() : "medgemma-intake";
    }
}
