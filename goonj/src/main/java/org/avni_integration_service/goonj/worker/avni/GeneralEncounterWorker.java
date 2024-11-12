package org.avni_integration_service.goonj.worker.avni;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.GeneralEncountersResponse;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.avni.repository.AvniEncounterRepository;
import org.avni_integration_service.avni.repository.AvniIgnoredConceptsRepository;
import org.avni_integration_service.avni.repository.AvniSubjectRepository;
import org.avni_integration_service.avni.worker.ErrorRecordWorker;
import org.avni_integration_service.goonj.GoonjEntityType;
import org.avni_integration_service.goonj.GoonjErrorType;
import org.avni_integration_service.goonj.config.GoonjContextProvider;
import org.avni_integration_service.goonj.exceptions.GoonjAvniRestException;
import org.avni_integration_service.goonj.repository.GoonjBaseRepository;
import org.avni_integration_service.goonj.service.AvniGoonjErrorService;
import org.avni_integration_service.goonj.util.DateTimeUtil;
import org.avni_integration_service.integration_data.domain.AvniEntityType;
import org.avni_integration_service.integration_data.domain.IntegratingEntityStatus;
import org.avni_integration_service.integration_data.domain.error.ErrorType;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.avni_integration_service.integration_data.service.error.ErrorClassifier;
import org.springframework.lang.NonNull;

import java.util.*;

import static org.avni_integration_service.goonj.config.GoonjConstants.*;

public abstract class GeneralEncounterWorker implements ErrorRecordWorker {
    private static final int INT_CONSTANT_ONE = 1;
    private final AvniEncounterRepository avniEncounterRepository;
    private final AvniSubjectRepository avniSubjectRepository;
    private final AvniIgnoredConceptsRepository avniIgnoredConceptsRepository;
    private final AvniGoonjErrorService avniGoonjErrorService;
    private final IntegratingEntityStatusRepository integrationEntityStatusRepository;

    private final GoonjErrorType goonjErrorType;
    private final GoonjEntityType entityType;
    private final String encounterType;
    private final Logger logger;
    private final ErrorClassifier errorClassifier;
    private final GoonjContextProvider goonjContextProvider;

    public GeneralEncounterWorker(AvniEncounterRepository avniEncounterRepository, AvniSubjectRepository avniSubjectRepository,
                                  AvniIgnoredConceptsRepository avniIgnoredConceptsRepository,
                                  AvniGoonjErrorService avniGoonjErrorService,
                                  IntegratingEntityStatusRepository integrationEntityStatusRepository,
                                  GoonjErrorType goonjErrorType, GoonjEntityType entityType, Logger logger,
                                  ErrorClassifier errorClassifier, GoonjContextProvider goonjContextProvider) {
        this.avniEncounterRepository = avniEncounterRepository;
        this.avniSubjectRepository = avniSubjectRepository;
        this.avniIgnoredConceptsRepository = avniIgnoredConceptsRepository;
        this.avniGoonjErrorService = avniGoonjErrorService;
        this.integrationEntityStatusRepository = integrationEntityStatusRepository;
        this.goonjErrorType = goonjErrorType;
        this.entityType = entityType;
        this.encounterType = entityType.getDbName();
        this.logger = logger;
        this.errorClassifier = errorClassifier;
        this.goonjContextProvider = goonjContextProvider;
    }

    public void processEncounters() throws Exception {
        processEncounters(Collections.emptyMap(), UPDATE_SYNC_STATUS_GOONJ_MAIN_JOB);
    }

    public void processEncounters(@NonNull Map<String, Object> filters, boolean updateSyncStatus) throws Exception {
        IntegratingEntityStatus status = integrationEntityStatusRepository.findByEntityType(encounterType);
        Date cutoffDateTime = getEffectiveCutoffDateTime(status);
        Object taskDateTimeFilter = filters.getOrDefault(FILTER_KEY_TIMESTAMP, cutoffDateTime);
        Date readUptoDateTime = Objects.nonNull(taskDateTimeFilter) && (taskDateTimeFilter instanceof Date)
                ? (Date) taskDateTimeFilter : cutoffDateTime; //Use db CutOffDateTime
        while (true) {
            GeneralEncountersResponse response = avniEncounterRepository.getGeneralEncounters(readUptoDateTime, encounterType);
            GeneralEncounter[] generalEncounters = response.getContent();
            int totalPages = response.getTotalPages();
            logger.info(String.format("Found %d encounters that are newer than %s", generalEncounters.length, readUptoDateTime));
            if (generalEncounters.length == 0) break;
            for (GeneralEncounter generalEncounter : generalEncounters) {
                processGeneralEncounter(generalEncounter, updateSyncStatus, goonjErrorType);
                readUptoDateTime = DateTimeUtil.convertToDate(generalEncounter.getLastModifiedDateTime().toString());
            }
            if (totalPages == INT_CONSTANT_ONE) {
                logger.info("Finished processing all pages");
                break;
            }
        }
    }

    /**
     *
     * @param status
     * @return EffectiveCutoffDateTime
     */
    private Date getEffectiveCutoffDateTime(IntegratingEntityStatus status) {
        return new Date(status.getReadUptoDateTime().toInstant().toEpochMilli());
    }

    @Override
    public void processError(String entityUuid) throws Exception {
        GeneralEncounter generalEncounter = avniEncounterRepository.getGeneralEncounter(entityUuid);
        if (generalEncounter == null) {
            String message = String.format("GeneralEncounter has been deleted now: %s", entityUuid);
            logger.warn(message);
            avniGoonjErrorService.errorOccurred(entityUuid, GoonjErrorType.EntityIsDeleted.name(), AvniEntityType.GeneralEncounter, message,null);
            return;
        }

        processGeneralEncounter(generalEncounter, false, goonjErrorType);
    }

    public void processGeneralEncounter(GeneralEncounter generalEncounter, boolean updateSyncStatus, GoonjErrorType goonjErrorType) throws Exception {
        removeIgnoredObservations(generalEncounter);
        logger.debug(String.format("Processing avni general encounter %s", generalEncounter.getUuid()));

        if (shouldFilterEncounter(generalEncounter)) {
            logger.warn(String.format("General encounter should be filtered out: %s", generalEncounter.getUuid()));
            updateErrorRecordAndSyncStatus(generalEncounter, updateSyncStatus, generalEncounter.getUuid());
            return;
        }

        var subject = avniSubjectRepository.getSubject(generalEncounter.getSubjectId());
        logger.debug(String.format("Found avni subject %s", subject.getUuid()));
        if (subject.getVoided()) {
            logger.debug(String.format("Avni subject is voided. Skipping. %s", subject.getUuid()));
            updateErrorRecordAndSyncStatus(generalEncounter, updateSyncStatus, generalEncounter.getUuid());
            return;
        }

        try {
            createOrUpdateGeneralEncounter(generalEncounter, subject);
            updateErrorRecordAndSyncStatus(generalEncounter, updateSyncStatus, generalEncounter.getUuid());
        }
        catch (GoonjAvniRestException exception){
            handleError(generalEncounter, exception, updateSyncStatus, goonjErrorType,exception.getErrorBody());
        }
        catch (Exception e) {
            handleError(generalEncounter, e, updateSyncStatus, goonjErrorType,null);
        }
    }

    protected void handleError(GeneralEncounter generalEncounter, Exception exception,
                               boolean updateSyncStatus, GoonjErrorType goonjErrorType,Map<String,Object> requestBody) throws Exception {
        logger.error(String.format("Avni encounter %s could not be synced to Goonj Salesforce. ", generalEncounter.getUuid()), exception);
        ErrorType classifiedErrorType = errorClassifier.classify(goonjContextProvider.get().getIntegrationSystem(),
                exception, goonjContextProvider.get().getBypassErrors(), GoonjErrorType.UnclassifiedError.name());
        if (classifiedErrorType == null) {
            throw exception;
        }
        createOrUpdateErrorRecordAndSyncStatus(generalEncounter, updateSyncStatus, generalEncounter.getUuid(),
                classifiedErrorType.getName(), exception.getLocalizedMessage(),requestBody);
    }

    protected abstract void createOrUpdateGeneralEncounter(GeneralEncounter generalEncounter, Subject subject);

    protected void syncEncounterToGoonj(Subject subject, GeneralEncounter generalEncounter, GoonjBaseRepository repository, String encounterTypeId) {
        HashMap<String, Object>[] response = repository.createEvent(subject, generalEncounter);
        logger.debug(String.format("%s %s synced successfully. ", encounterTypeId, response[0].get(encounterTypeId)));
    }

    private void removeIgnoredObservations(GeneralEncounter generalEncounter) {
        var observations = generalEncounter.getObservations();
        avniIgnoredConceptsRepository.getIgnoredConcepts().forEach(observations::remove);
        generalEncounter.setObservations(observations);
    }

    private boolean shouldFilterEncounter(GeneralEncounter generalEncounter) {
        return !generalEncounter.isCompleted() || generalEncounter.getVoided();
    }

    private void updateErrorRecordAndSyncStatus(GeneralEncounter generalEncounter, boolean updateSyncStatus, String sid) {
        avniGoonjErrorService.successfullyProcessed(sid, entityType);
        updateSyncStatus(generalEncounter, updateSyncStatus);
    }

    private void createOrUpdateErrorRecordAndSyncStatus(GeneralEncounter generalEncounter, boolean updateSyncStatus, String sid, String goonjErrorTypeName, String errorMsg,Map<String,Object> requestBody) {
        avniGoonjErrorService.errorOccurred(sid, goonjErrorTypeName, entityType, "Subject IDs: " + generalEncounter.getSubjectExternalID() + ", " + generalEncounter.getSubjectId() + ". Message: " + errorMsg,requestBody );
        updateSyncStatus(generalEncounter, updateSyncStatus);
    }

    private void updateSyncStatus(GeneralEncounter generalEncounter, boolean updateSyncStatus) {
        if (updateSyncStatus) {
            updateReadUptoDateTime(generalEncounter);
        }
    }

    private void updateReadUptoDateTime(GeneralEncounter generalEncounter) {
        IntegratingEntityStatus intEnt = integrationEntityStatusRepository.findByEntityType(encounterType);
        intEnt.setReadUptoDateTime(DateTimeUtil.convertToDate(generalEncounter.getLastModifiedDateTime().toString()));
        integrationEntityStatusRepository.save(intEnt);
    }

    public void performAllProcesses() throws Exception {
        processEncounters();
    }

    /**
     * To be invoked by GoonjAdhocTask Jobs only
     * @param filters
     *       {
     *          "dateTimestamp": "2024-10-10 12:34:56.123456Z",
     *       }
     * @throws Exception
     */
    public void performAllProcesses(Map<String, Object> filters) throws Exception {
        processEncounters(filters, UPDATE_SYNC_STATUS_GOONJ_ADHOC_JOB);
    }
}
