package org.avni_integration_service.wati.config;

import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfig;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WatiConfigTest {

    private WatiConfig watiConfig;

    private WatiConfig configWith(String... keyValues) {
        List<IntegrationSystemConfig> configs = new java.util.ArrayList<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            IntegrationSystemConfig c = new IntegrationSystemConfig();
            c.setKey(keyValues[i]);
            c.setValue(keyValues[i + 1]);
            configs.add(c);
        }
        IntegrationSystem integrationSystem = new IntegrationSystem();
        integrationSystem.setId(1);
        return new WatiConfig(new IntegrationSystemConfigCollection(configs), integrationSystem);
    }

    // --- getTemplateName ---

    @Test
    public void templateName_usesLocaleSpecificWhenAvailable() {
        WatiConfig config = configWith(
                "flow.weekly_survey.template_name", "weekly_en",
                "flow.weekly_survey.template_name.te", "weekly_te"
        );
        assertEquals("weekly_te", config.getTemplateName("weekly_survey", "te"));
    }

    @Test
    public void templateName_fallsBackToDefaultWhenLocaleKeyMissing() {
        WatiConfig config = configWith(
                "flow.weekly_survey.template_name", "weekly_en"
        );
        assertEquals("weekly_en", config.getTemplateName("weekly_survey", "te"));
    }

    @Test
    public void templateName_usesDefaultWhenLocaleIsNull() {
        WatiConfig config = configWith(
                "flow.weekly_survey.template_name", "weekly_en"
        );
        assertEquals("weekly_en", config.getTemplateName("weekly_survey", null));
    }

    @Test
    public void templateName_usesDefaultWhenLocaleIsEmpty() {
        WatiConfig config = configWith(
                "flow.weekly_survey.template_name", "weekly_en"
        );
        assertEquals("weekly_en", config.getTemplateName("weekly_survey", ""));
    }

    // --- getFlowToQueryMap ---

    @Test
    public void flowToQueryMap_returnsCorrectMapping() {
        WatiConfig config = configWith(
                "flow.weekly_survey.custom_query", "dil_weekly_survey_scheduled_today",
                "flow.weekly_survey.enabled", "true",
                "flow.biweekly_payment.custom_query", "dil_biweekly_payment_query"
        );
        var map = config.getFlowToQueryMap();
        assertEquals(2, map.size());
        assertEquals("dil_weekly_survey_scheduled_today", map.get("weekly_survey"));
        assertEquals("dil_biweekly_payment_query", map.get("biweekly_payment"));
    }

    @Test
    public void flowToQueryMap_ignoresNonQueryKeys() {
        WatiConfig config = configWith(
                "flow.weekly_survey.custom_query", "dil_query",
                "flow.weekly_survey.cooldown_days", "6"
        );
        assertEquals(1, config.getFlowToQueryMap().size());
    }

    // --- getFlowConfig defaults ---

    @Test
    public void flowConfig_usesDefaultsWhenNotConfigured() {
        WatiConfig config = configWith(
                "flow.weekly_survey.custom_query", "dil_query"
        );
        WatiFlowConfig flowConfig = config.getFlowConfig("weekly_survey");
        assertEquals(7, flowConfig.getCooldownDays());
        assertEquals(3, flowConfig.getMaxRetries());
        assertEquals(24, flowConfig.getRetryIntervalHours());
    }

    @Test
    public void flowConfig_usesConfiguredValues() {
        WatiConfig config = configWith(
                "flow.weekly_survey.custom_query", "dil_query",
                "flow.weekly_survey.cooldown_days", "6",
                "flow.weekly_survey.max_retries", "5",
                "flow.weekly_survey.retry_interval_hours", "12"
        );
        WatiFlowConfig flowConfig = config.getFlowConfig("weekly_survey");
        assertEquals(6, flowConfig.getCooldownDays());
        assertEquals(5, flowConfig.getMaxRetries());
        assertEquals(12, flowConfig.getRetryIntervalHours());
    }
}
