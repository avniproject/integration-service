package org.avni_integration_service.goonj.repository;

import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.avni.domain.AvniBaseContract;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.goonj.GoonjEntityType;
import org.avni_integration_service.goonj.config.GoonjContextProvider;
import org.avni_integration_service.goonj.domain.ActivityConstants;
import org.avni_integration_service.goonj.dto.ActivityDTO;
import org.avni_integration_service.goonj.dto.ActivityRequestDTO;
import org.avni_integration_service.goonj.util.DateTimeUtil;
import org.avni_integration_service.integration_data.domain.MappingMetaData;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.avni_integration_service.integration_data.repository.MappingMetaDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

import static org.avni_integration_service.goonj.config.GoonjMappingDbConstants.*;
import static org.avni_integration_service.goonj.util.NumberUtil.getInteger;

@Component("ActivityRepository")
public class ActivityRepository extends GoonjBaseRepository implements ActivityConstants {

    protected static final Logger logger = LoggerFactory.getLogger(ActivityRepository.class);
    private final MappingMetaDataRepository mappingMetaDataRepository;

    @Autowired
    public ActivityRepository(IntegratingEntityStatusRepository integratingEntityStatusRepository,
                              @Qualifier("GoonjRestTemplate") RestTemplate restTemplate,
                              MappingMetaDataRepository mappingMetaDataRepository,
                              AvniHttpClient avniHttpClient, GoonjContextProvider goonjContextProvider) {
        super(integratingEntityStatusRepository, restTemplate,
                GoonjEntityType.Activity.name(), avniHttpClient, goonjContextProvider);
        this.mappingMetaDataRepository = mappingMetaDataRepository;
    }

    @Override
    public HashMap<String, Object>[] fetchEvents(Map<String, Object> filters) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> fetchDeletionEvents(Map<String, Object> filters) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HashMap<String, Object>[] createEvent(Subject subject) {
        ActivityRequestDTO requestDTO = convertSubjectToActivityRequest(subject);
        HttpEntity<ActivityRequestDTO> request = new HttpEntity<>(requestDTO);
        return super.createSingleEntity(RESOURCE_ACTIVITY, request);
    }

    @Override
    public HashMap<String, Object>[] createEvent(Subject subject, GeneralEncounter encounter) {
        throw new UnsupportedOperationException();
    }

    private ActivityRequestDTO convertSubjectToActivityRequest(Subject subject) {
        ActivityRequestDTO requestDTO = new ActivityRequestDTO();
        requestDTO.setActivities(Arrays.asList(createActivityRequest(subject)));
        return requestDTO;
    }

    private ActivityDTO createActivityRequest(Subject subject) {
        ActivityDTO activityDTO = new ActivityDTO();
        /* Activity ID and relationship fields */
        activityDTO.setSourceId(subject.getUuid());
        /* Activity location fields */
        HashMap<String, String> location = (HashMap<String, String>) subject.get(LOCATION);
        activityDTO.setState(location.get(STATE));
        activityDTO.setDistrict(location.get(DISTRICT));
        if (location.get(BLOCK).equals("Other")) {
            activityDTO.setOtherBlock((String) subject.getObservation(OTHER_BLOCK));
        }
        activityDTO.setBlock(location.get(BLOCK));
        if (location.get(VILLAGE).equals("Other")) {
            activityDTO.setOtherVillage((String) subject.getObservation(OTHER_VILLAGE));
        }
        activityDTO.setLocalityVillageName(location.get(VILLAGE));
        activityDTO.setTolaMohalla((String) subject.getObservation(TOLA_MOHALLA));
        /* Activity Account fields */
        activityDTO.setnameOfAccount((String) subject.getObservation(ACCOUNT_NAME));
        /* Activity description fields */
        activityDTO.setTypeofInitiative((String) subject.getObservation(TYPE_OF_INITIATIVE));
        /* Activity Date fields */
        Date activityEndDate = DateTimeUtil.convertToDate((String) subject.getObservation(ACTIVITY_END_DATE));
        activityEndDate = DateTimeUtil.offsetTimeZone(activityEndDate, DateTimeUtil.UTC, DateTimeUtil.IST);
        activityDTO.setActivityEndDate(DateTimeUtil.formatDate(activityEndDate));
        Date activityStartDate = DateTimeUtil.convertToDate((String) subject.getObservation(ACTIVITY_START_DATE));
        activityStartDate = DateTimeUtil.offsetTimeZone(activityStartDate, DateTimeUtil.UTC, DateTimeUtil.IST);
        activityDTO.setActivityStartDate(DateTimeUtil.formatDate(activityStartDate));
        List<String> studentActivities = (ArrayList<String>) subject.getObservation(ACTIVITY_CONDUCTED_WITH_STUDENTS);
        if (studentActivities != null) {
            activityDTO.setActivityConductedWithStudents(String.join(";", studentActivities));
        }
        activityDTO.setOtherDetails((String) subject.getObservation(OTHER_ACTIVITY));
        activityDTO.setSchoolAanganwadiLearningCenterName((String) subject.getObservation(SCHOOL_AANGANWADI_LEARNINGCENTER_NAME));

        if (subject.getObservation(TYPE_OF_INITIATIVE).equals("CFW")) {
            /* Participation fields */
            activityDTO.setNoofWorkingDays((subject.getObservation(NUMBER_OF_WORKING_DAYS) == null) ? 0L : (Integer) subject.getObservation(NUMBER_OF_WORKING_DAYS));
            activityDTO.setNoofparticipantsMaleCFW((Integer) subject.getObservation(NUMBER_OF_PARTICIPANTS_MALE));
            activityDTO.setNoofparticipantsFemaleCFW((Integer) subject.getObservation(NUMBER_OF_PARTICIPANTS_FEMALE));
            activityDTO.setNoofparticipantsCFWOther((Integer) subject.getObservation(NUMBER_OF_PARTICIPANTS_OTHER));
            /* Activity description fields */
            activityDTO.setActivityCategory((String) subject.getObservation(ACTIVITY_CATEGORY));
            mapActivityType(activityDTO, subject);
            activityDTO.setActivitySubType((String) subject.getObservation(ACTIVITY_SUB_TYPE));
            activityDTO.setOtherSubType((String) subject.getObservation(SPECIFY_OTHER_SUB_TYPE));
            activityDTO.setFormCrossChecked((String) subject.getObservation(FORM_CROSS_CHECKED));
            /*changed 'Objective of Work' to 'Work Objective'*/
            List<String> workObjective = (ArrayList<String>) subject.getObservation(WORK_OBJECTIVE);
            if(workObjective != null){
                activityDTO.setObjectiveofCFWwork(String.join(";", workObjective));
            }else {
                activityDTO.setObjectiveofCFWwork((String) subject.getObservation(OBJECTIVE_OF_WORK));
            }
            activityDTO.setOtherObjective((String) subject.getObservation(SPECIFY_OTHER_FOR_OBJECTIVE_OF_WORK));
            /* Measurement fields */
            activityDTO.setMeasurementType((String) subject.getObservation(MEASUREMENTS_TYPE));
            activityDTO.setNos(getInteger((subject.getObservation(NOS))));
            activityDTO.setBreadth((subject.getObservation(BREADTH) == null) ? 0D : Double.valueOf(subject.getObservation(BREADTH).toString()));
            activityDTO.setDiameter((subject.getObservation(DIAMETER) == null) ? 0D : Double.valueOf(subject.getObservation(DIAMETER).toString()));
            activityDTO.setLength((subject.getObservation(LENGTH) == null) ? 0D : Double.valueOf(subject.getObservation(LENGTH).toString()));
            activityDTO.setDepthHeight((subject.getObservation(HEIGHT_DEPTH) == null) ? 0D : Double.valueOf(subject.getObservation(HEIGHT_DEPTH).toString()));
            /* Photograph fields */
            activityDTO.setBeforeImplementationPhotograph(getPhotographStrings(BEFORE_IMPLEMENTATION_PHOTOGRAPH, subject));
            activityDTO.setDuringImplementationPhotograph(getPhotographStrings(DURING_IMPLEMENTATION_PHOTOGRAPH, subject));
            activityDTO.setAfterImplementationPhotograph(getPhotographStrings(AFTER_IMPLEMENTATION_PHOTOGRAPH, subject));
        }
        if (subject.getObservation(TYPE_OF_INITIATIVE).equals("S2S")) {
            /* Participation fields */
            activityDTO.setNoofparticipantsS2S((subject.getObservation(NUMBER_OF_PARTICIPANTS) == null) ? 0L : (Integer) subject.getObservation(NUMBER_OF_PARTICIPANTS));
            activityDTO.setNoofdaysofParticipationS2S((subject.getObservation(NUMBER_OF_DAYS_OF_PARTICIPATION) == null) ? 0L : (Integer) subject.getObservation(NUMBER_OF_DAYS_OF_PARTICIPATION));
            activityDTO.setSchoolAanganwadiLearningCenterName((String) subject.getObservation(SCHOOL_AANGANWADI_LEARNINGCENTER_NAME));
            /* Photograph fields */
            activityDTO.setS2sPhotograph(getPhotographStrings(PHOTOGRAPH, subject));
            /* Activity description fields */
            activityDTO.setTypeOfSchool((String) subject.getObservation(TYPE_OF_SCHOOL));
        }
        if (subject.getObservation(TYPE_OF_INITIATIVE).equals("NJPC")) {
            /* Participation fields */
            activityDTO.setNoofdaysofParticipationNJPC((subject.getObservation(NUMBER_OF_DAYS_OF_PARTICIPATION) == null) ? 0L : (Integer) subject.getObservation(NUMBER_OF_DAYS_OF_PARTICIPATION));
            activityDTO.setNoofparticipantsMaleNJPC((Integer) subject.getObservation(NUMBER_OF_PARTICIPANTS_MALE));
            activityDTO.setNoofparticipantsFemaleNJPC((Integer) subject.getObservation(NUMBER_OF_PARTICIPANTS_FEMALE));
            activityDTO.setNoofparticipantsNJPCOther((Integer) subject.getObservation(NUMBER_OF_PARTICIPANTS_OTHER));
            /* Photograph fields */
            activityDTO.setNjpcPhotograph(getPhotographStrings(PHOTOGRAPH, subject));
            /*Education fields*/
            String educationAndHealth = (String) subject.getObservation(EDUCATION_AND_HEALTH);
            if(educationAndHealth!=null && educationAndHealth.equals(YES)){
                activityDTO.setEducationAndHealth(true);
                activityDTO.setMaleStudent((subject.getObservation(MALE_STUDENT) == null) ? 0 : (int)subject.getObservation(MALE_STUDENT));
                activityDTO.setFemaleStudent((subject.getObservation(FEMALE_STUDENT) == null) ? 0 : (int)subject.getObservation(FEMALE_STUDENT));
                int male = (subject.getObservation(MALE_STUDENT)==null) ? 0 : (int)subject.getObservation(MALE_STUDENT);
                int female = (subject.getObservation(FEMALE_STUDENT)==null) ? 0 : (int)subject.getObservation(FEMALE_STUDENT);
                activityDTO.setNoofparticipantsNJPC(Long.valueOf((subject.getObservation(NUMBER_OF_PARTICIPANTS) == null) ? (male+female) : (Integer) subject.getObservation(NUMBER_OF_PARTICIPANTS)));
                activityDTO.setSchoolAanganwadiLearningCenterName((String)subject.getObservation(SCHOOL_AANGANWADI_LEARNINGCENTER_NAME));
            }
            else{
                activityDTO.setEducationAndHealth(false);
            }
        }
        activityDTO.setWasUndertakingFormFilled((String) subject.getObservation(WAS_DISCLAIMER_FORM_FILLED));
        activityDTO.setRemarks((String) subject.getObservation(REMARKS));
        /* Other fields */
        activityDTO.setCreatedBy(subject.getCreatedBy());
        activityDTO.setModifiedBy(subject.getLastModifiedBy());

        return activityDTO;
    }

    protected void mapActivityType(ActivityDTO activityDTO, AvniBaseContract subject) {
        if (subject.getObservation(ACTIVITY_TYPE) != null) {
            int integrationSystemId = goonjContextProvider.get().getIntegrationSystem().getId();
            MappingMetaData answerMapping = mappingMetaDataRepository.getIntSystemMappingIfPresent(MappingGroup_Activity, MappingType_Obs,
                    (String) subject.getObservation(ACTIVITY_TYPE), integrationSystemId);
            if (answerMapping != null) {
                activityDTO.setActivityType(answerMapping.getIntSystemValue());
            }
        }
    }

    private String getPhotographStrings(String photo, Subject subject) {
        Object imageObservation = subject.getObservation(photo);
        if (imageObservation == null) {
            return null;
        }
        String mediaUrl = goonjContextProvider.get().getMediaUrl();
        if (imageObservation instanceof List) {
            return ((ArrayList<String>) imageObservation).stream()
                    .map(image -> mediaUrl + image)
                    .collect(Collectors.joining(";"));
        } else {
            return mediaUrl + imageObservation;
        }
    }
}
