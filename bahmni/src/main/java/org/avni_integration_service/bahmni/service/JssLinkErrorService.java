package org.avni_integration_service.bahmni.service;

import org.avni_integration_service.integration_data.domain.AvniEntityType;
import org.avni_integration_service.bahmni.BahmniErrorType;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.error.ErrorRecord;
import org.avni_integration_service.integration_data.domain.error.ErrorRecordLog;
import org.avni_integration_service.integration_data.domain.error.ErrorType;
import org.avni_integration_service.integration_data.repository.ErrorRecordRepository;
import org.avni_integration_service.integration_data.repository.ErrorTypeRepository;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Problems with the Bahmni ID on an Avni individual's "Individual ID Update" form, kept as one error record per
 * individual so they can be reported to the JSSCP team (Metabase query: JSS Bahmni integration/reports/bahmni-link-errors.sql). Cleared automatically once the individual is linked.
 * Records are marked processing-disabled: they are re-checked when the ID Update form changes, never by the
 * generic error-retry job (which would push them through the old Avni -> Bahmni subject sync).
 */
@Service
public class JssLinkErrorService {
    public static final List<BahmniErrorType> LINK_ERROR_TYPES = List.of(BahmniErrorType.BahmniPatientNotFound,
            BahmniErrorType.MultipleBahmniPatientsWithId, BahmniErrorType.BahmniIdOnMultiplePeople, BahmniErrorType.BahmniPatientLinkedToAnotherPerson);

    private final ErrorRecordRepository errorRecordRepository;
    private final ErrorTypeRepository errorTypeRepository;
    private final IntegrationSystemRepository integrationSystemRepository;

    public JssLinkErrorService(ErrorRecordRepository errorRecordRepository, ErrorTypeRepository errorTypeRepository, IntegrationSystemRepository integrationSystemRepository) {
        this.errorRecordRepository = errorRecordRepository;
        this.errorTypeRepository = errorTypeRepository;
        this.integrationSystemRepository = integrationSystemRepository;
    }

    @Transactional
    public void record(String avniSubjectUuid, BahmniErrorType type, String message, Map<String, Object> details) {
        ErrorType errorType = errorTypeRepository.findByNameAndIntegrationSystem(type.name(), bahmni());
        ErrorRecord errorRecord = errorRecordRepository.findByAvniEntityTypeAndEntityId(AvniEntityType.Subject, avniSubjectUuid);
        if (errorRecord == null) {
            errorRecord = new ErrorRecord();
            errorRecord.setAvniEntityType(AvniEntityType.Subject);
            errorRecord.setEntityId(avniSubjectUuid);
            errorRecord.setIntegrationSystem(bahmni());
        }
        errorRecord.setProcessingDisabled(true);
        if (errorRecord.getLastErrorRecordLog() != null && errorRecord.hasThisAsLastErrorTypeAndErrorMessage(errorType, message)) {
            errorRecord.updateLoggedAtForLastErrorRecordLog(details);
        } else {
            errorRecord.addErrorLog(errorType, message, details);
        }
        errorRecordRepository.save(errorRecord);
    }

    /** Removes the individual's link error, if any (leaves other kinds of errors alone). */
    @Transactional
    public void clear(String avniSubjectUuid) {
        ErrorRecord errorRecord = errorRecordRepository.findByAvniEntityTypeAndEntityId(AvniEntityType.Subject, avniSubjectUuid);
        if (errorRecord != null && isLinkError(errorRecord.getLastErrorRecordLog())) errorRecordRepository.delete(errorRecord);
    }

    private static boolean isLinkError(ErrorRecordLog log) {
        return log != null && log.getErrorType() != null
                && LINK_ERROR_TYPES.stream().map(Enum::name).anyMatch(name -> name.equals(log.getErrorType().getName()));
    }

    private IntegrationSystem bahmni() {
        return integrationSystemRepository.findBySystemType(IntegrationSystem.IntegrationSystemType.bahmni);
    }
}
