package org.avni_integration_service.goonj.worker;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.SyncDirection;
import org.avni_integration_service.avni.worker.ErrorRecordWorker;
import org.avni_integration_service.goonj.GoonjEntityType;
import org.avni_integration_service.goonj.config.GoonjConstants;
import org.avni_integration_service.goonj.config.GoonjContextProvider;
import org.avni_integration_service.goonj.service.AvniGoonjErrorService;
import org.avni_integration_service.goonj.worker.avni.ActivityWorker;
import org.avni_integration_service.goonj.worker.avni.DispatchReceiptWorker;
import org.avni_integration_service.goonj.worker.avni.DistributionWorker;
import org.avni_integration_service.goonj.worker.goonj.DemandEventWorker;
import org.avni_integration_service.goonj.worker.goonj.DispatchEventWorker;
import org.avni_integration_service.goonj.worker.goonj.InventoryEventWorker;
import org.avni_integration_service.integration_data.domain.IntegratingEntityStatus;
import org.avni_integration_service.integration_data.domain.error.ErrorRecord;
import org.avni_integration_service.integration_data.domain.error.ErrorRecordLog;
import org.avni_integration_service.integration_data.domain.error.ErrorTypeFollowUpStep;
import org.avni_integration_service.integration_data.repository.ErrorRecordLogRepository;
import org.avni_integration_service.integration_data.repository.ErrorRecordRepository;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AvniGoonjErrorRecordsWorker {

    @Autowired
    private ErrorRecordRepository errorRecordRepository;
    @Autowired
    private AvniGoonjErrorService avniGoonjErrorService;
    @Autowired
    private DemandEventWorker demandEventWorker;
    @Autowired
    private DispatchEventWorker dispatchEventWorker;
    @Autowired
    private DispatchReceiptWorker dispatchReceiptWorker;
    @Autowired
    private DistributionWorker distributionWorker;
    @Autowired
    private ActivityWorker activityWorker;
    @Autowired
    private InventoryEventWorker inventoryEventWorker;
    @Autowired
    private GoonjContextProvider goonjContextProvider;
    @Autowired
    private IntegratingEntityStatusRepository integrationEntityStatusRepository;
    @Autowired
    private ErrorRecordLogRepository errorRecordLogRepository;

    private static final Logger logger = Logger.getLogger(AvniGoonjErrorRecordsWorker.class);

    private static final int pageSize = 20;

    public void process(SyncDirection syncDirection, boolean allErrors) throws Exception {
        Page<ErrorRecord> errorRecordPage;
        int pageNumber = 0;
        do {
            logger.info(String.format("Starting page number: %d for sync direction: %s", pageNumber, syncDirection.name()));
            PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);
            int integrationSystemId = goonjContextProvider.get().getIntegrationSystem().getId();
            if (syncDirection.equals(SyncDirection.AvniToGoonj))
                errorRecordPage = errorRecordRepository.findAllByAvniEntityTypeNotNullAndProcessingDisabledFalseAndErrorRecordLogsErrorTypeNotInAndIntegrationSystemIdOrderById(
                        avniGoonjErrorService.getUnprocessableErrorTypes(), integrationSystemId, pageRequest);
            else if (syncDirection.equals(SyncDirection.GoonjToAvni) && !allErrors)
                errorRecordPage = errorRecordRepository.findAllByIntegratingEntityTypeNotNullAndProcessingDisabledFalseAndErrorRecordLogsErrorTypeNotInAndIntegrationSystemIdOrderById(
                        avniGoonjErrorService.getUnprocessableErrorTypes(), integrationSystemId, pageRequest);
            else if (syncDirection.equals(SyncDirection.GoonjToAvni) && allErrors)
                errorRecordPage = errorRecordRepository.findAllByIntegratingEntityTypeNotNullAndErrorRecordLogsErrorTypeNotInAndIntegrationSystemIdOrderById(
                        avniGoonjErrorService.getUnprocessableErrorTypes(), integrationSystemId, pageRequest);
            else {
                throw new RuntimeException("Invalid arguments");
            }

            List<ErrorRecord> errorRecords = errorRecordPage.getContent().stream()
                    .distinct()
                    .filter(er -> !er.hasThisAsLastErrorTypeFollowUpStep(ErrorTypeFollowUpStep.Terminal))
                    .collect(Collectors.toList());
            for (ErrorRecord errorRecord : errorRecords) {
                retryErroredEntitySync(errorRecord);
            }
            logger.info(String.format("Completed page number: %d with number of errors: %d, for sync direction: %s", pageNumber, errorRecords.size(), syncDirection.name()));
            pageNumber++;
        } while (errorRecordPage.getNumberOfElements() == pageSize);
    }

    private void retryErroredEntitySync(ErrorRecord errorRecord) {
        ErrorRecordWorker errorRecordWorker = getErrorRecordWorker(errorRecord);
        try {
            errorRecordWorker.processError(errorRecord.getEntityId());
        } catch (Exception exception) {
            logger.error(String.format("Failed to process errorRecord of type: %s with entityId : %s ",
                    errorRecord.getIntegratingEntityType(), errorRecord.getEntityId()), exception);
        }
    }

    private ErrorRecordWorker getErrorRecordWorker(ErrorRecord errorRecord) {
        if (errorRecord.getIntegratingEntityType() != null) {
            if (errorRecord.getIntegratingEntityType().equals(GoonjEntityType.DispatchReceipt.name())) return dispatchReceiptWorker;
            if (errorRecord.getIntegratingEntityType().equals(GoonjEntityType.Distribution.name())) return distributionWorker;
            if (errorRecord.getIntegratingEntityType().equals(GoonjEntityType.Activity.name())) return activityWorker;
            if (errorRecord.getIntegratingEntityType().equals(GoonjEntityType.Demand.name())) return demandEventWorker;
            if (errorRecord.getIntegratingEntityType().equals(GoonjEntityType.Dispatch.name())) return dispatchEventWorker;
            if (errorRecord.getIntegratingEntityType().equals(GoonjEntityType.Inventory.name())) return inventoryEventWorker;
        }
        throw new AssertionError(String.format("Invalid error record with AvniEntityType=%s / GoonjEntityType=%s", errorRecord.getAvniEntityType(), errorRecord.getIntegratingEntityType()));
    }

    public Map<ErrorTypeFollowUpStep, Long> evaluateNewErrors() {
        IntegratingEntityStatus goonjErrorRecordLogIES = integrationEntityStatusRepository.findByEntityType(GoonjConstants.GoonjErrorRecordLog);
        ErrorRecordLog lastErrorRecordLog = errorRecordLogRepository.findTopByOrderByLoggedAtDesc();
        int integrationSystemId = goonjContextProvider.get().getIntegrationSystem().getId();
        Map<ErrorTypeFollowUpStep, Long> errorTypeFollowUpStepLongMap = new HashMap<>();
        for (ErrorTypeFollowUpStep followUpStep: ErrorTypeFollowUpStep.values()) {
            long numberOfErrors = errorRecordLogRepository.countByLoggedAtIsBetweenAndErrorTypeFollowUpStepAndErrorRecordIntegrationSystemId(
                    goonjErrorRecordLogIES.getReadUptoDateTime(), lastErrorRecordLog.getLoggedAt(), String.valueOf(followUpStep.ordinal()), integrationSystemId);
            errorTypeFollowUpStepLongMap.put(followUpStep, numberOfErrors);
        }
        goonjErrorRecordLogIES.setReadUptoDateTime(lastErrorRecordLog.getLoggedAt());
        integrationEntityStatusRepository.save(goonjErrorRecordLogIES);
        return errorTypeFollowUpStepLongMap;
    }
}
