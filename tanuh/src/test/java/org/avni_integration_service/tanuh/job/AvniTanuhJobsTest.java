package org.avni_integration_service.tanuh.job;

import com.bugsnag.Bugsnag;
import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.integration_data.context.IntegrationContext;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.avni_integration_service.tanuh.config.TanuhAvniSessionFactory;
import org.avni_integration_service.tanuh.config.TanuhConfig;
import org.avni_integration_service.tanuh.config.TanuhContextProvider;
import org.avni_integration_service.tanuh.worker.OralScreeningWorker;
import org.avni_integration_service.util.HealthCheckService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AvniTanuhJobsTest {
    @Mock
    private Bugsnag bugsnag;
    @Mock
    private HealthCheckService healthCheckService;
    @Mock
    private TanuhAvniSessionFactory sessionFactory;
    @Mock
    private AvniHttpClient avniHttpClient;
    @Mock
    private OralScreeningWorker worker;
    private final TanuhContextProvider contextProvider = new TanuhContextProvider();

    private TanuhConfig config(String name) {
        IntegrationSystem system = new IntegrationSystem();
        system.setId(7);
        ReflectionTestUtils.setField(system, "name", name);
        return new TanuhConfig(new IntegrationSystemConfigCollection(List.of()), system);
    }

    @AfterEach
    public void tearDown() {
        TanuhContextProvider.clear();
        IntegrationContext.removeContext();
    }

    private List<String> recordContextsWhenPinged(String slug) {
        List<String> seen = new ArrayList<>();
        doAnswer(invocation -> {
            seen.add(contextProvider.get().getIntegrationSystem().getName());
            seen.add(IntegrationContext.get().getName());
            return null;
        }).when(healthCheckService).success(slug);
        return seen;
    }

    private void assertContextsCleared() {
        assertThrows(IllegalStateException.class, contextProvider::get);
        assertThrows(IllegalStateException.class, IntegrationContext::get);
    }

    @Test
    public void mainJobRunsInItsOrganisationsContextAndPingsItsOwnCheck() {
        AvniTanuhMainJob job = new AvniTanuhMainJob(bugsnag, healthCheckService, sessionFactory, avniHttpClient, contextProvider, worker);
        List<String> seen = recordContextsWhenPinged("tanuh_uat_local");
        List<String> seenByWorker = new ArrayList<>();
        doAnswer(invocation -> {
            seenByWorker.add(IntegrationContext.get().getName());
            return null;
        }).when(worker).processNew();

        job.execute(config("Tanuh_UAT_Local"));

        assertEquals(List.of("Tanuh_UAT_Local", "Tanuh_UAT_Local"), seen);
        assertEquals(List.of("Tanuh_UAT_Local"), seenByWorker);
        verify(avniHttpClient).setAvniSession(any());
        verify(healthCheckService, never()).failure(anyString());
        assertContextsCleared();
    }

    @Test
    public void mainJobPingsFailureReportsAndClearsWhenTheRunFails() {
        AvniTanuhMainJob job = new AvniTanuhMainJob(bugsnag, healthCheckService, sessionFactory, avniHttpClient, contextProvider, worker);
        RuntimeException failure = new RuntimeException("sign-in refused");
        when(sessionFactory.createSession()).thenThrow(failure);

        job.execute(config("Tanuh_UAT_Local"));

        verify(healthCheckService).failure("tanuh_uat_local");
        verify(healthCheckService, never()).success(anyString());
        verify(bugsnag).notify(failure);
        assertContextsCleared();
    }

    @Test
    public void mainJobClearsItsContextsWhenWorkAfterSetupFails() {
        AvniTanuhMainJob job = new AvniTanuhMainJob(bugsnag, healthCheckService, sessionFactory, avniHttpClient, contextProvider, worker);
        RuntimeException failure = new RuntimeException("scoring failed");
        doThrow(failure).when(healthCheckService).success("tanuh_uat_local");

        job.execute(config("Tanuh_UAT_Local"));

        verify(healthCheckService).failure("tanuh_uat_local");
        verify(bugsnag).notify(failure);
        assertContextsCleared();
    }

    @Test
    public void mainJobPingsFailureWhenTheWorkerThrows() {
        AvniTanuhMainJob job = new AvniTanuhMainJob(bugsnag, healthCheckService, sessionFactory, avniHttpClient, contextProvider, worker);
        IllegalStateException failure = new IllegalStateException("No TanuhOralScreening cursor row for this organisation. Run its setup script.");
        doThrow(failure).when(worker).processNew();

        job.execute(config("Tanuh_UAT_Local"));

        verify(healthCheckService).failure("tanuh_uat_local");
        verify(healthCheckService, never()).success(anyString());
        verify(bugsnag).notify(failure);
        assertContextsCleared();
    }

    @Test
    public void errorJobRunsInItsOrganisationsContextAndPingsTheErrorCheck() {
        AvniTanuhErrorJob job = new AvniTanuhErrorJob(bugsnag, healthCheckService, sessionFactory, avniHttpClient, contextProvider);
        List<String> seen = recordContextsWhenPinged("tanuh_prod_local-error");

        job.execute(config("Tanuh_Prod_Local"));

        assertEquals(List.of("Tanuh_Prod_Local", "Tanuh_Prod_Local"), seen);
        assertContextsCleared();
    }

    @Test
    public void errorJobPingsTheErrorCheckWhenTheRunFails() {
        AvniTanuhErrorJob job = new AvniTanuhErrorJob(bugsnag, healthCheckService, sessionFactory, avniHttpClient, contextProvider);
        RuntimeException failure = new RuntimeException("sign-in refused");
        when(sessionFactory.createSession()).thenThrow(failure);

        job.execute(config("Tanuh_Prod_Local"));

        verify(healthCheckService).failure("tanuh_prod_local-error");
        verify(bugsnag).notify(failure);
        assertContextsCleared();
    }
}
