package org.avni_integration_service.tanuh.worker;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.repository.AvniEncounterRepository;
import org.avni_integration_service.avni.worker.ErrorRecordWorker;
import org.avni_integration_service.integration_data.domain.AvniEntityType;
import org.avni_integration_service.integration_data.domain.error.ErrorRecord;
import org.avni_integration_service.integration_data.domain.error.ErrorTypeFollowUpStep;
import org.avni_integration_service.integration_data.repository.ErrorRecordRepository;
import org.avni_integration_service.tanuh.config.TanuhContextProvider;
import org.avni_integration_service.tanuh.domain.TanuhConcepts;
import org.avni_integration_service.tanuh.service.TanuhErrorService;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Date;
import java.util.List;

// Retries every screening waiting in an error record, as the screening is now, until it is scored or no longer needs
// scoring. The main job leaves a screening alone while its record is open, so this is the only path that scores it.
@Component
public class TanuhScreeningRetryWorker implements ErrorRecordWorker {
    private static final Logger logger = Logger.getLogger(TanuhScreeningRetryWorker.class);

    private final ErrorRecordRepository errorRecordRepository;
    private final AvniEncounterRepository avniEncounterRepository;
    private final OralScreeningWorker oralScreeningWorker;
    private final TanuhErrorService tanuhErrorService;
    private final TanuhContextProvider tanuhContextProvider;

    public TanuhScreeningRetryWorker(ErrorRecordRepository errorRecordRepository, AvniEncounterRepository avniEncounterRepository,
                                     OralScreeningWorker oralScreeningWorker, TanuhErrorService tanuhErrorService,
                                     TanuhContextProvider tanuhContextProvider) {
        this.errorRecordRepository = errorRecordRepository;
        this.avniEncounterRepository = avniEncounterRepository;
        this.oralScreeningWorker = oralScreeningWorker;
        this.tanuhErrorService = tanuhErrorService;
        this.tanuhContextProvider = tanuhContextProvider;
    }

    // True when no retry in the run failed. A failed retry keeps its record, with a fresh log, for the next run.
    public boolean processWaiting() {
        List<ErrorRecord> waiting = errorRecordRepository.getProcessableErrorRecords();
        if (waiting.isEmpty()) return true;
        confirmAvniAnswers();
        boolean noneFailed = true;
        for (ErrorRecord errorRecord : waiting) {
            if (errorRecord.hasThisAsLastErrorTypeFollowUpStep(ErrorTypeFollowUpStep.Terminal)) continue;
            noneFailed &= retry(errorRecord);
        }
        return noneFailed;
    }

    @Override
    public void processError(String entityUuid) {
        ErrorRecord errorRecord = errorRecordRepository.findByAvniEntityTypeAndEntityId(AvniEntityType.GeneralEncounter, entityUuid);
        if (errorRecord == null) return;
        confirmAvniAnswers();
        if (!retry(errorRecord))
            throw new IllegalStateException(String.format("Screening %s is still waiting: its retry failed", entityUuid));
    }

    private boolean retry(ErrorRecord errorRecord) {
        String uuid = errorRecord.getEntityId();
        GeneralEncounter screening;
        try {
            screening = avniEncounterRepository.getGeneralEncounter(uuid);
        } catch (HttpClientErrorException.NotFound e) {
            // Trusted only because the run's first call succeeded, so the address and the sign-in are good.
            logger.warn(String.format("Screening %s is not on the server any more", uuid));
            return settle(errorRecord, "no longer on the server");
        } catch (RuntimeException e) {
            logger.error(String.format("Screening %s could not be read for its retry", uuid), e);
            tanuhErrorService.errorOccurred(uuid, TanuhErrorService.SCREENING_PROCESSING_FAILED, String.format("%s: %s", e.getClass().getSimpleName(), e.getMessage()));
            return false;
        }
        if (Boolean.TRUE.equals(screening.getVoided())) return settle(errorRecord, "deleted");
        if (tanuhContextProvider.get().getAvniImplUser().equalsIgnoreCase(screening.getLastModifiedBy()))
            return settle(errorRecord, "already written by the job");
        return switch (oralScreeningWorker.processScreening(screening, false)) {
            case WRITTEN -> settle(errorRecord, "scored");
            case SKIPPED -> settle(errorRecord, "no longer needs scoring");
            // It changed while being retried, so nothing was written: it keeps waiting and the newer version is retried next run.
            case CHANGED -> true;
            // The error service has refreshed the record's log.
            case FAILED -> false;
        };
    }

    // Sign-in happens on the run's first call, and the client passes a 404 through unchanged. A wrong address, or a
    // proxy answering 404, would otherwise read as "screening gone" and drop every waiting record behind a green
    // check. This call lists nothing and must succeed before any record is touched; its failure fails the run.
    private void confirmAvniAnswers() {
        avniEncounterRepository.getGeneralEncounters(new Date(), TanuhConcepts.ORAL_SCREENING, 1);
    }

    private boolean settle(ErrorRecord errorRecord, String why) {
        errorRecordRepository.delete(errorRecord);
        logger.info(String.format("Screening %s stopped waiting: %s", errorRecord.getEntityId(), why));
        return true;
    }
}
