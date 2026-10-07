package org.avni_integration_service.scheduler;

import org.avni_integration_service.amrit.job.AvniAmritFullErrorJob;
import org.avni_integration_service.amrit.job.AvniAmritMainJob;
import org.avni_integration_service.goonj.job.AvniGoonjFullErrorJob;
import org.avni_integration_service.goonj.job.AvniGoonjMainJob;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfig;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.avni_integration_service.integration_data.repository.config.IntegrationSystemConfigRepository;
import org.avni_integration_service.job.AvniPowerFullErrorJob;
import org.avni_integration_service.job.AvniPowerMainJob;
import org.avni_integration_service.lahi.job.AvniLahiFullErrorJob;
import org.avni_integration_service.lahi.job.AvniLahiMainJob;
import org.avni_integration_service.rwb.job.AvniRwbMainJob;
import org.avni_integration_service.tanuh.config.TanuhConfig;
import org.avni_integration_service.tanuh.job.AvniTanuhErrorJob;
import org.avni_integration_service.tanuh.job.AvniTanuhMainJob;
import org.avni_integration_service.wati.job.AvniWatiErrorJob;
import org.avni_integration_service.wati.job.AvniWatiMainJob;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class IntegrationJobSchedulerTanuhTest {
    private static final String MAIN_CRON = "0 */15 * * * ?";
    private static final String ERROR_CRON = "0 5/15 * * * ?";

    private final TaskScheduler taskScheduler = mock(TaskScheduler.class);
    private final IntegrationSystemRepository systemRepository = mock(IntegrationSystemRepository.class);
    private final IntegrationSystemConfigRepository configRepository = mock(IntegrationSystemConfigRepository.class);
    private final AvniTanuhMainJob tanuhMainJob = mock(AvniTanuhMainJob.class);
    private final AvniTanuhErrorJob tanuhErrorJob = mock(AvniTanuhErrorJob.class);

    private IntegrationJobScheduler scheduler(String currentEnvironment) {
        IntegrationJobScheduler scheduler = new IntegrationJobScheduler(
                mock(AvniGoonjMainJob.class), mock(AvniGoonjFullErrorJob.class),
                mock(AvniPowerMainJob.class), mock(AvniPowerFullErrorJob.class),
                mock(AvniLahiMainJob.class), mock(AvniLahiFullErrorJob.class),
                mock(AvniAmritMainJob.class), mock(AvniAmritFullErrorJob.class),
                mock(AvniRwbMainJob.class), mock(AvniWatiMainJob.class), mock(AvniWatiErrorJob.class),
                tanuhMainJob, tanuhErrorJob,
                taskScheduler, configRepository, systemRepository);
        ReflectionTestUtils.setField(scheduler, "currentEnvironment", currentEnvironment);
        return scheduler;
    }

    private IntegrationSystem tanuhRow(int id, String name, String... keyValues) {
        IntegrationSystem row = new IntegrationSystem();
        row.setId(id);
        ReflectionTestUtils.setField(row, "name", name);
        ReflectionTestUtils.setField(row, "systemType", IntegrationSystem.IntegrationSystemType.tanuh);
        List<IntegrationSystemConfig> configs = new ArrayList<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            IntegrationSystemConfig config = new IntegrationSystemConfig();
            config.setKey(keyValues[i]);
            config.setValue(keyValues[i + 1]);
            configs.add(config);
        }
        when(configRepository.getInstanceConfiguration(row)).thenReturn(new IntegrationSystemConfigCollection(configs));
        return row;
    }

    @Test
    public void eachTanuhOrganisationGetsItsOwnMainAndRetryJob() {
        IntegrationSystem uat = tanuhRow(1, "tanuh_uat_local", "int_env", "local", "main.scheduled.job.cron", MAIN_CRON, "error.scheduled.job.cron", ERROR_CRON);
        IntegrationSystem prod = tanuhRow(2, "tanuh_prod_local", "int_env", "local", "main.scheduled.job.cron", MAIN_CRON, "error.scheduled.job.cron", ERROR_CRON);
        when(systemRepository.findAllBySystemType(IntegrationSystem.IntegrationSystemType.tanuh)).thenReturn(List.of(uat, prod));

        scheduler("local").scheduleAll();

        ArgumentCaptor<Runnable> runnables = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Trigger> triggers = ArgumentCaptor.forClass(Trigger.class);
        verify(taskScheduler, times(4)).schedule(runnables.capture(), triggers.capture());
        assertEquals(List.of(MAIN_CRON, ERROR_CRON, MAIN_CRON, ERROR_CRON),
                triggers.getAllValues().stream().map(t -> ((CronTrigger) t).getExpression()).toList());

        runnables.getAllValues().forEach(Runnable::run);
        ArgumentCaptor<TanuhConfig> mainConfigs = ArgumentCaptor.forClass(TanuhConfig.class);
        ArgumentCaptor<TanuhConfig> errorConfigs = ArgumentCaptor.forClass(TanuhConfig.class);
        verify(tanuhMainJob, times(2)).execute(mainConfigs.capture());
        verify(tanuhErrorJob, times(2)).execute(errorConfigs.capture());
        assertEquals(List.of("tanuh_uat_local", "tanuh_prod_local"),
                mainConfigs.getAllValues().stream().map(c -> c.getIntegrationSystem().getName()).toList());
        assertEquals(List.of("tanuh_uat_local", "tanuh_prod_local"),
                errorConfigs.getAllValues().stream().map(c -> c.getIntegrationSystem().getName()).toList());
    }

    @Test
    public void anOrganisationSetForAnotherEnvironmentGetsNoJobs() {
        IntegrationSystem prod = tanuhRow(2, "tanuh_prod_local", "int_env", "prod", "main.scheduled.job.cron", MAIN_CRON, "error.scheduled.job.cron", ERROR_CRON);
        when(systemRepository.findAllBySystemType(IntegrationSystem.IntegrationSystemType.tanuh)).thenReturn(List.of(prod));

        scheduler("local").scheduleAll();

        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Trigger.class));
    }

    @Test
    public void anOrganisationWithoutSchedulesGetsNoJobs() {
        IntegrationSystem uat = tanuhRow(1, "tanuh_uat_local", "int_env", "local");
        when(systemRepository.findAllBySystemType(IntegrationSystem.IntegrationSystemType.tanuh)).thenReturn(List.of(uat));

        scheduler("local").scheduleAll();

        verify(taskScheduler, never()).schedule(any(Runnable.class), any(Trigger.class));
    }
}
