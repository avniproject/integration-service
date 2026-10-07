package org.avni_integration_service.tanuh.service;

import org.avni_integration_service.integration_data.domain.AvniEntityType;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.config.IntegrationSystemConfigCollection;
import org.avni_integration_service.integration_data.domain.error.ErrorRecord;
import org.avni_integration_service.integration_data.domain.error.ErrorRecordLog;
import org.avni_integration_service.integration_data.domain.error.ErrorType;
import org.avni_integration_service.integration_data.repository.ErrorRecordRepository;
import org.avni_integration_service.integration_data.repository.ErrorTypeRepository;
import org.avni_integration_service.tanuh.config.TanuhConfig;
import org.avni_integration_service.tanuh.config.TanuhContextProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TanuhErrorServiceTest {
    private static final String SCREENING = "a4312f00-2cd9-41a5-8e38-d6520a151009";
    @Mock
    private ErrorRecordRepository errorRecordRepository;
    @Mock
    private ErrorTypeRepository errorTypeRepository;
    private final TanuhContextProvider contextProvider = new TanuhContextProvider();
    private TanuhErrorService service;
    private ErrorType modelCallFailed;

    @BeforeEach
    public void setUp() {
        IntegrationSystem system = new IntegrationSystem();
        system.setId(15);
        ReflectionTestUtils.setField(system, "name", "tanuh_uat_local");
        contextProvider.set(new TanuhConfig(new IntegrationSystemConfigCollection(List.of()), system));
        modelCallFailed = new ErrorType("HighRiskModelCallFailed", system);
        service = new TanuhErrorService(errorRecordRepository, errorTypeRepository, contextProvider);
    }

    @AfterEach
    public void tearDown() {
        TanuhContextProvider.clear();
    }

    @Test
    public void aFirstFailureCreatesARecordOnTheAvniEntityColumn() {
        when(errorTypeRepository.findByNameAndIntegrationSystemId("HighRiskModelCallFailed", 15)).thenReturn(modelCallFailed);

        service.errorOccurred(SCREENING, "HighRiskModelCallFailed", "The stand-in model is set to fail");

        ArgumentCaptor<ErrorRecord> saved = ArgumentCaptor.forClass(ErrorRecord.class);
        verify(errorRecordRepository).saveErrorRecord(saved.capture());
        ErrorRecord record = saved.getValue();
        assertEquals(AvniEntityType.GeneralEncounter, record.getAvniEntityType());
        assertEquals(SCREENING, record.getEntityId());
        assertNull(record.getIntegratingEntityType());
        assertFalse(record.isProcessingDisabled());
        assertEquals(1, record.getErrorRecordLogs().size());
        ErrorRecordLog log = record.getLastErrorRecordLog();
        assertEquals("HighRiskModelCallFailed", log.getErrorType().getName());
        assertEquals("The stand-in model is set to fail", log.getErrorMsg());
    }

    private ErrorRecord existingWith(ErrorType type, String message) {
        ErrorRecord existing = new ErrorRecord();
        existing.setAvniEntityType(AvniEntityType.GeneralEncounter);
        existing.setEntityId(SCREENING);
        existing.addErrorLog(type, message, null);
        existing.getLastErrorRecordLog().setLoggedAt(new Date(0));
        when(errorRecordRepository.findByAvniEntityTypeAndEntityId(AvniEntityType.GeneralEncounter, SCREENING)).thenReturn(existing);
        return existing;
    }

    @Test
    public void aRepeatOfTheSameFailureRefreshesItsLog() {
        when(errorTypeRepository.findByNameAndIntegrationSystemId("HighRiskModelCallFailed", 15)).thenReturn(modelCallFailed);
        ErrorRecord existing = existingWith(modelCallFailed, "The stand-in model is set to fail");

        service.errorOccurred(SCREENING, "HighRiskModelCallFailed", "The stand-in model is set to fail");

        verify(errorRecordRepository).save(existing);
        verify(errorRecordRepository, never()).saveErrorRecord(any());
        assertEquals(1, existing.getErrorRecordLogs().size());
        assertTrue(existing.getLastErrorRecordLog().getLoggedAt().after(new Date(0)));
    }

    // ErrorRecordLog is equal by record and error type, so a record holds one log per type: a new message replaces it.
    @Test
    public void aNewMessageOfTheSameTypeReplacesThatLogsMessage() {
        when(errorTypeRepository.findByNameAndIntegrationSystemId("HighRiskModelCallFailed", 15)).thenReturn(modelCallFailed);
        ErrorRecord existing = existingWith(modelCallFailed, "The stand-in model is set to fail");

        service.errorOccurred(SCREENING, "HighRiskModelCallFailed", "The stand-in model never scores a screening on production");

        verify(errorRecordRepository).save(existing);
        assertEquals(1, existing.getErrorRecordLogs().size());
        assertEquals("The stand-in model never scores a screening on production", existing.getLastErrorRecordLog().getErrorMsg());
        assertTrue(existing.getLastErrorRecordLog().getLoggedAt().after(new Date(0)));
    }

    @Test
    public void aFailureOfAnotherTypeAddsALogToTheSameRecord() {
        IntegrationSystem system = new IntegrationSystem();
        system.setId(15);
        ErrorType processingFailed = new ErrorType("ScreeningProcessingFailed", system);
        when(errorTypeRepository.findByNameAndIntegrationSystemId("HighRiskModelCallFailed", 15)).thenReturn(modelCallFailed);
        ErrorRecord existing = existingWith(processingFailed, "403 Forbidden");

        service.errorOccurred(SCREENING, "HighRiskModelCallFailed", "The stand-in model is set to fail");

        verify(errorRecordRepository).save(existing);
        verify(errorRecordRepository, never()).saveErrorRecord(any());
        assertEquals(2, existing.getErrorRecordLogs().size());
        assertEquals("HighRiskModelCallFailed", existing.getLastErrorRecordLog().getErrorType().getName());
    }

    @Test
    public void aMissingErrorTypeFailsLoudly() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> service.errorOccurred(SCREENING, "ScreeningProcessingFailed", "403 Forbidden"));
        assertTrue(e.getMessage().contains("ScreeningProcessingFailed") && e.getMessage().contains("setup script"), e.getMessage());
        verifyNoInteractions(errorRecordRepository);
    }
}
