package org.avni_integration_service.tanuh.config;

import org.apache.log4j.Logger;
import org.avni_integration_service.integration_data.context.ContextIntegrationSystem;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

public class TanuhConfig {
    private static final Logger logger = Logger.getLogger(TanuhConfig.class);

    public static final double DEFAULT_SAFETY_SAMPLE_RATE = 0.05;
    public static final String DEFAULT_STUB_MODE = "by_encounter";
    private static final List<String> STUB_MODES = List.of("fixed", "by_encounter", "fail");
    private static final List<String> STUB_RESULTS = List.of("High Risk", "Low Risk", "Non Suspicious");

    private final IntegrationSystemConfigCollection integrationSystemConfigCollection;
    private final ContextIntegrationSystem integrationSystem;
    private final double safetySampleRate;
    private final String stubMode;
    private final String stubFixedResult;

    public TanuhConfig(IntegrationSystemConfigCollection integrationSystemConfigCollection, IntegrationSystem integrationSystem) {
        this.integrationSystemConfigCollection = integrationSystemConfigCollection;
        this.integrationSystem = new ContextIntegrationSystem(integrationSystem);
        this.safetySampleRate = readSafetySampleRate();
        this.stubMode = readStubMode();
        this.stubFixedResult = readStubFixedResult();
    }

    private String getStringConfigValue(String key, String defaultValue) {
        String configValue = integrationSystemConfigCollection.getConfigValue(key);
        return StringUtils.hasLength(configValue) ? configValue : defaultValue;
    }

    private double readSafetySampleRate() {
        String value = getStringConfigValue("safety_sample_rate", null);
        if (!StringUtils.hasText(value)) {
            logger.warn(String.format("[%s] safety_sample_rate is not set, using %s", integrationSystem.getName(), DEFAULT_SAFETY_SAMPLE_RATE));
            return DEFAULT_SAFETY_SAMPLE_RATE;
        }
        try {
            double rate = Double.parseDouble(value.trim());
            if (rate >= 0.0 && rate <= 1.0) return rate;
            logger.warn(String.format("[%s] safety_sample_rate '%s' is not from 0 to 1, using %s", integrationSystem.getName(), value, DEFAULT_SAFETY_SAMPLE_RATE));
        } catch (NumberFormatException e) {
            logger.warn(String.format("[%s] safety_sample_rate '%s' is not a number, using %s", integrationSystem.getName(), value, DEFAULT_SAFETY_SAMPLE_RATE));
        }
        return DEFAULT_SAFETY_SAMPLE_RATE;
    }

    private String readStubMode() {
        String value = getStringConfigValue("model_stub_mode", null);
        if (!StringUtils.hasText(value)) return DEFAULT_STUB_MODE;
        if (STUB_MODES.contains(value.trim())) return value.trim();
        logger.warn(String.format("[%s] model_stub_mode '%s' is not one of %s, using %s", integrationSystem.getName(), value, STUB_MODES, DEFAULT_STUB_MODE));
        return DEFAULT_STUB_MODE;
    }

    private String readStubFixedResult() {
        String value = getStringConfigValue("model_stub_fixed_result", null);
        if (!StringUtils.hasText(value)) return null;
        if (STUB_RESULTS.contains(value.trim())) return value.trim();
        logger.warn(String.format("[%s] model_stub_fixed_result '%s' is not one of %s", integrationSystem.getName(), value, STUB_RESULTS));
        return null;
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

    public double getSafetySampleRate() {
        return safetySampleRate;
    }

    public String getStubMode() {
        return stubMode;
    }

    public String getStubFixedResult() {
        return stubFixedResult;
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
}
