package org.avni_integration_service.tanuh.service;

import com.bugsnag.Bugsnag;
import com.bugsnag.Report;
import com.bugsnag.Severity;
import com.bugsnag.callbacks.Callback;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.avni_integration_service.tanuh.config.TanuhConfig;
import org.avni_integration_service.tanuh.config.TanuhContextProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class TanuhAlertsTest {
    private final Bugsnag bugsnag = mock(Bugsnag.class);
    private final TanuhContextProvider context = new TanuhContextProvider();
    private final TanuhAlerts alerts = new TanuhAlerts(bugsnag, context);

    @BeforeEach
    public void setUp() {
        IntegrationSystem system = new IntegrationSystem();
        system.setId(15);
        ReflectionTestUtils.setField(system, "name", "tanuh_uat");
        context.set(new TanuhConfig(new IntegrationSystemConfigCollection(List.of()), system));
    }

    @AfterEach
    public void tearDown() {
        TanuhContextProvider.clear();
    }

    @Test
    public void aScreeningThatNeedsAPersonIsReportedAsAWarningGroupedByItsReason() {
        alerts.screeningNeedsAPerson("s1", TanuhAlerts.Reason.REFUSED, "{\"error\":\"INVALID_MANIFEST\"}");

        ArgumentCaptor<Throwable> error = ArgumentCaptor.forClass(Throwable.class);
        ArgumentCaptor<Callback> callback = ArgumentCaptor.forClass(Callback.class);
        verify(bugsnag).notify(error.capture(), eq(Severity.WARNING), callback.capture());
        assertTrue(error.getValue() instanceof TanuhAlerts.ScreeningNeedsAPersonException, error.getValue().getClass().getName());
        assertEquals("Screening s1 was refused by Tanuh's model service, so it will not be scored; the call was made, so it is marked OCSHR Call Made Yes and not sent again: {\"error\":\"INVALID_MANIFEST\"}",
                error.getValue().getMessage());

        Report report = mock(Report.class);
        callback.getValue().beforeNotify(report);
        verify(report).setGroupingHash("tanuh:REFUSED");
        verify(report).setContext("tanuh_uat");
    }

    @Test
    public void eachReasonIsItsOwnGroup() {
        alerts.screeningNeedsAPerson("s1", TanuhAlerts.Reason.HELD_WITH_OTHER_PHOTOS, "{}");

        ArgumentCaptor<Throwable> error = ArgumentCaptor.forClass(Throwable.class);
        ArgumentCaptor<Callback> callback = ArgumentCaptor.forClass(Callback.class);
        verify(bugsnag).notify(error.capture(), eq(Severity.WARNING), callback.capture());
        assertEquals("Screening s1 is held by Tanuh's model service with other photos, which are the ones it scores; the call was made, so it is marked OCSHR Call Made Yes and not sent again: {}",
                error.getValue().getMessage());
        Report report = mock(Report.class);
        callback.getValue().beforeNotify(report);
        verify(report).setGroupingHash("tanuh:HELD_WITH_OTHER_PHOTOS");
    }
}
