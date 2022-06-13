package org.avni_integration_service.goonj.worker;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.SyncDirection;
import org.avni_integration_service.avni.worker.ErrorRecordWorker;
import org.avni_integration_service.goonj.GoonjEntityType;
import org.avni_integration_service.goonj.service.AvniGoonjErrorService;
import org.avni_integration_service.goonj.worker.goonj.DemandEventWorker;
import org.avni_integration_service.integration_data.domain.Constants;
import org.avni_integration_service.integration_data.domain.error.ErrorRecord;
import org.avni_integration_service.integration_data.repository.ErrorRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AvniGoonjErrorRecordsWorker {
    @Autowired
    private ErrorRecordRepository errorRecordRepository;
    @Autowired
    private AvniGoonjErrorService avniGoonjErrorService;
    @Autowired
    private DemandEventWorker demandEventWorker;

    private static final Logger logger = Logger.getLogger(AvniGoonjErrorRecordsWorker.class);

    private static final int pageSize = 20;

    public void process(SyncDirection syncDirection, boolean allErrors) {
        Page<ErrorRecord> errorRecordPage;
        int pageNumber = 0;
        do {
            logger.info(String.format("Starting page number: %d for sync direction: %s", pageNumber, syncDirection.name()));
            PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);
            if (syncDirection.equals(SyncDirection.AvniToGoonj))
                errorRecordPage = errorRecordRepository.findAllByAvniEntityTypeNotNullAndProcessingDisabledFalseAndErrorRecordLogsErrorTypeNotInOrderById(avniGoonjErrorService.getUnprocessableErrorTypes(), pageRequest);
            else if (syncDirection.equals(SyncDirection.GoonjToAvni) && !allErrors)
                errorRecordPage = errorRecordRepository.findAllByIntegratingEntityTypeNotNullAndProcessingDisabledFalseAndErrorRecordLogsErrorTypeNotInOrderById(avniGoonjErrorService.getUnprocessableErrorTypes(), pageRequest);
            else if (syncDirection.equals(SyncDirection.GoonjToAvni) && allErrors)
                errorRecordPage = errorRecordRepository.findAllByIntegratingEntityTypeNotNullAndErrorRecordLogsErrorTypeNotInOrderById(avniGoonjErrorService.getUnprocessableErrorTypes(), pageRequest);
            else
                throw new RuntimeException("Invalid arguments");

            List<ErrorRecord> errorRecords = errorRecordPage.getContent();
            for (ErrorRecord errorRecord : errorRecords) {
                ErrorRecordWorker errorRecordWorker = getErrorRecordWorker(errorRecord);
                errorRecordWorker.processError(errorRecord.getEntityId());
            }
            logger.info(String.format("Completed page number: %d with number of errors: %d, for sync direction: %s", pageNumber, errorRecords.size(), syncDirection.name()));
            pageNumber++;
        } while (errorRecordPage.getNumberOfElements() == pageSize);
    }

    private ErrorRecordWorker getErrorRecordWorker(ErrorRecord errorRecord) {
        if (errorRecord.getAvniEntityType() != null) {
//            if (errorRecord.getAvniEntityType().equals(AvniEntityType.Subject)) return subjectWorker;
//            if (errorRecord.getAvniEntityType().equals(AvniEntityType.Enrolment)) return enrolmentWorker;
//            if (errorRecord.getAvniEntityType().equals(AvniEntityType.ProgramEncounter)) return programEncounterWorker;
//            if (errorRecord.getAvniEntityType().equals(AvniEntityType.GeneralEncounter)) return generalEncounterWorker;
        } else if (errorRecord.getIntegratingEntityType() != null) {
            //TODO
//            if (errorRecord.getIntegratingEntityType().equals(GoonjEntityType.Activity)) return activityEventWorker;
              if (errorRecord.getIntegratingEntityType().equals(GoonjEntityType.Demand)) return demandEventWorker;
//            if (errorRecord.getIntegratingEntityType().equals(GoonjEntityType.Dispatch)) return dispatchEncounterEventWorker;
//            if (errorRecord.getIntegratingEntityType().equals(GoonjEntityType.DispatchReceipt)) return dispatchReceiptEventWorker;
//            if (errorRecord.getIntegratingEntityType().equals(GoonjEntityType.Distribution)) return distributionEncounterEventWorker;
        }
        throw new AssertionError(String.format("Invalid error record with AvniEntityType=%s and GoonjEntityType=%s", errorRecord.getAvniEntityType(), errorRecord.getIntegratingEntityType()));
    }

    public void cacheRunImmutables(Constants constants) {
//        subjectWorker.cacheRunImmutables(constants);
//        enrolmentWorker.cacheRunImmutables(constants);
//        programEncounterWorker.cacheRunImmutables(constants);
//        generalEncounterWorker.cacheRunImmutables(constants);
        //TODO
//        activityEventWorker.cacheRunImmutables(constants);
        demandEventWorker.cacheRunImmutables(constants);
//        dispatchEncounterEventWorker.cacheRunImmutables(constants);
//        dispatchReceiptEventWorker.cacheRunImmutables(constants);
//        distributionEncounterEventWorker.cacheRunImmutables(constants);
    }
}
