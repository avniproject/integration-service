package org.avni_integration_service.goonj.repository;

import com.nimbusds.oauth2.sdk.util.StringUtils;
import org.avni_integration_service.avni.client.AvniHttpClient;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.avni.repository.AvniSubjectRepository;
import org.avni_integration_service.goonj.GoonjEntityType;
import org.avni_integration_service.goonj.config.GoonjContextProvider;
import org.avni_integration_service.goonj.domain.DistributionConstants;
import org.avni_integration_service.goonj.dto.*;
import org.avni_integration_service.goonj.util.DateTimeUtil;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Component("DistributionRepository")
public class DistributionRepository extends GoonjBaseRepository implements DistributionConstants {

    public static final String WEB_MEDIA = "/web/media";
    private AvniSubjectRepository avniSubjectRepository;

    @Autowired
    public DistributionRepository(IntegratingEntityStatusRepository integratingEntityStatusRepository,
                                  @Qualifier("GoonjRestTemplate") RestTemplate restTemplate,
                                  AvniHttpClient avniHttpClient,
                                  AvniSubjectRepository avniSubjectRepository, GoonjContextProvider goonjContextProvider) {
        super(integratingEntityStatusRepository, restTemplate,
                GoonjEntityType.Distribution.name(), avniHttpClient, goonjContextProvider);
        this.avniSubjectRepository = avniSubjectRepository;
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
    public HashMap<String, Object>[] createEvent(Subject subject, GeneralEncounter encounter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public HashMap<String, Object>[] createEvent(Subject subject) {
        DistributionRequestDTO requestDTO = convertSubjectToDistributionRequest(subject);
        HttpEntity<DistributionRequestDTO> request = new HttpEntity<>(requestDTO);
        return super.createSingleEntity(RESOURCE_DISTRIBUTION, request);
    }

    private DistributionRequestDTO convertSubjectToDistributionRequest(Subject subject) {
        DistributionRequestDTO requestDTO = new DistributionRequestDTO();
        requestDTO.setDistributions(List.of(createDistributionRequest(subject)));
        return requestDTO;
    }

    private DistributionDTO createDistributionRequest(Subject subject) {
        DistributionDTO distributionDTO = new DistributionDTO();
//        String dispatchStatusId = (String) encounter.getObservation(DISPATCH_STATUS_ID_NEW);
//        if(!StringUtils.hasText(dispatchStatusId)) {
//            dispatchStatusId = (String) encounter.getObservation(DISPATCH_STATUS_ID_OLD);
//        }
//        distributionDTO.setDispatchStatus(dispatchStatusId);
        distributionDTO.setSource_id(subject.getUuid());
        /* Distribution location fields */
        HashMap<String, String> location = (HashMap<String, String>) subject.get(LOCATION);
        distributionDTO.setState(location.get(STATE));
        distributionDTO.setDistrict(location.get(DISTRICT));
        if (location.get(BLOCK).equals("Other")) {
            distributionDTO.setOtherBlock((String) subject.getObservation(OTHER_BLOCK));
        }
        distributionDTO.setBlock(location.get(BLOCK));
        if (location.get(VILLAGE).equals("Other")) {
            distributionDTO.setOtherVillage((String) subject.getObservation(OTHER_VILLAGE));
        }
        distributionDTO.setLocalityVillageName(location.get(VILLAGE));
        distributionDTO.setTolaMohalla((String) subject.getObservation(TOLA_MOHALLA));
        /* Distribution Account fields */
        distributionDTO.setNameOfAccount((String) subject.getObservation(ACCOUNT_NAME));
        /* Distribution Related fields*/
        Date distributionDate = DateTimeUtil.convertToDate((String) subject.getObservation(DISTRIBUTION_DATE));
        distributionDate = DateTimeUtil.offsetTimeZone(distributionDate, DateTimeUtil.UTC, DateTimeUtil.IST);
        distributionDTO.setDateOfDistribution(DateTimeUtil.formatDate(distributionDate));
        if(subject.getObservation(TYPE_OF_COMMUNITY)!= null){
            List<String> typeOfCommunity = (ArrayList<String>) subject.getObservation(TYPE_OF_COMMUNITY);
            distributionDTO.setTypeOfCommunity(String.join(";",typeOfCommunity));
        }else{
          distributionDTO.setTypeOfCommunity((String) subject.getObservation(TARGET_COMMUNITY));
        }
        distributionDTO.setDisasterType((String) subject.getObservation(TYPE_OF_DISASTER));
        List<String> images = subject.getObservation(IMAGES) == null ? new ArrayList<>() : (ArrayList<String>) subject.getObservation(IMAGES);
        distributionDTO.setPhotographInformation(images.stream().map(
                x -> goonjContextProvider.get().getMediaUrl() + x).collect(Collectors.joining(";")));
        List<String> disclaimerImage = subject.getObservation(DISCLAIMER_PHOTOGRAPHS) == null ? new ArrayList<>() : (ArrayList<String>) subject.getObservation(DISCLAIMER_PHOTOGRAPHS);
        distributionDTO.setUndertakingFormPhotographs(disclaimerImage.stream().map(
                x -> goonjContextProvider.get().getMediaUrl() + x).collect(Collectors.joining(";")));
        List<String> receiverImages = subject.getObservation(RECEIVER_LIST_PHOTOGRAPHS) == null ? new ArrayList<>() : (ArrayList<String>) subject.getObservation(RECEIVER_LIST_PHOTOGRAPHS);
        distributionDTO.setReceiverListPhotographs(receiverImages.stream().map(
                x -> goonjContextProvider.get().getMediaUrl() + x).collect(Collectors.joining(";")));
        List<DistributionLine> d = fetchDistributionLineItems(subject);
        distributionDTO.setDistributionLines(d);
        List<DistributionActivities> activities = fetchActivities(subject);
        distributionDTO.setActivities(activities);
        distributionDTO.setTotalNumberOfReceivers((Integer) subject.getObservation(NUMBER_OF_RECEIVERS));
        if (subject.getObservation(TYPE_OF_INITIATIVE).equals(CFW)) {
            distributionDTO.setTypeOfInitiative(ONLY_CFW);
        } else if (subject.getObservation(TYPE_OF_INITIATIVE).equals(NJPC)) {
            distributionDTO.setTypeOfInitiative(ONLY_NJPC);
        } else if (subject.getObservation(TYPE_OF_INITIATIVE).equals(RAHAT)) {
            distributionDTO.setTypeOfInitiative(ONLY_RAHAT);
        } else if (subject.getObservation(TYPE_OF_INITIATIVE).equals(S_2_S)) {
            distributionDTO.setTypeOfInitiative(ONLY_S_2_S);
            distributionDTO.setTypeOfSchool((String) subject.getObservation(TYPE_OF_SCHOOL));
            distributionDTO.setSchoolAanganwadiLearningCenterName((String) subject.getObservation(SCHOOL_ANGANWADI_NAME));
        } else if (subject.getObservation(TYPE_OF_INITIATIVE).equals(CFW_S2S)) {
            distributionDTO.setTypeOfInitiative((String) subject.getObservation(TYPE_OF_INITIATIVE));
            distributionDTO.setTypeOfSchool((String) subject.getObservation(TYPE_OF_SCHOOL));
            distributionDTO.setSchoolAanganwadiLearningCenterName((String) subject.getObservation(SCHOOL_ANGANWADI_NAME));
        }
        /* vaapsi fields */
        else if (subject.getObservation(TYPE_OF_INITIATIVE).equals(VAAPSI)) {
            distributionDTO.setTypeOfInitiative((String) subject.getObservation(TYPE_OF_INITIATIVE));
            distributionDTO.setSurveyedBy((String) subject.getObservation(SURVEYED_BY));
            distributionDTO.setMonitoredByOrDistributor((String) subject.getObservation(MONITORED_BY_DISTRIBUTOR));
            distributionDTO.setApprovedOrVerifiedBy((String) subject.getObservation(APPROVED_OR_VERIFIED_BY));
            distributionDTO.setTeamOrExternal((String) subject.getObservation(TEAM_OR_EXTERNAL));
            distributionDTO.setNameOfPOC((String) subject.getObservation(POC_NAME));
            Map<String, Object> contactObs = (Map<String, Object>) subject.getObservation(POC_CONTACT_NO);
            distributionDTO.setPocContactNo((String) contactObs.get("phoneNumber"));
            distributionDTO.setReachedTo((String) subject.getObservation(REACHED_TO));
            distributionDTO.setAnyOtherDocumentSubmitted((String) subject.getObservation(ANY_OTHER_DOCUMENT_SUBMITTED));
            if (subject.getObservation(REACHED_TO).equals("Individual")) {
                distributionDTO.setName((String) subject.getObservation(NAME));
                distributionDTO.setGender((String) subject.getObservation(GENDER));
                distributionDTO.setFatherMotherName((String) subject.getObservation(FATHER_MOTHER_NAME));
                distributionDTO.setAge((Integer) subject.getObservation(AGE));
                distributionDTO.setPhoneNumber((String) ((Map<String, Object>) subject.getObservation(POC_CONTACT_NO)).get("phoneNumber"));
                distributionDTO.setPresentOccupation((String) subject.getObservation(PRESENT_OCCUPATION));
                distributionDTO.setNoOfFamilyMember((Integer) subject.getObservation(NUMBER_OF_FAMILY_MEMBERS));
                distributionDTO.setMonthlyIncome((Integer) subject.getObservation(MONTHLY_INCOME));
            }
            if (subject.getObservation(REACHED_TO).equals("Group")) {
                distributionDTO.setGroupName((String) subject.getObservation(GROUP_NAME));
            }
        } else if (subject.getObservation(TYPE_OF_INITIATIVE).equals(SPECIFIC_INITIATIVE)) {
            distributionDTO.setTypeOfInitiative((String) subject.getObservation(TYPE_OF_INITIATIVE));
            distributionDTO.setCentreName((String) subject.getObservation(CENTERS_NAME));
            distributionDTO.setShareABriefProvidedMaterial((String) subject.getObservation(PROVIDED_MATERIAL));
            distributionDTO.setHowtheMaterialMakesaDifference((String) subject.getObservation(MATERIAL_MAKES_DIFFERENCE));
            distributionDTO.setMaterialGivenFor((String) subject.getObservation(MATERIAL_GIVEN_FOR));
            distributionDTO.setOtherDetails((String) subject.getObservation(ANY_OTHER_MATERIAL_GIVEN));
            distributionDTO.setNoOfFamiliesReached((Integer) subject.getObservation(NUMBER_OF_FAMILIES_REACHED));
            distributionDTO.setNoOfIndividualReached((Integer) subject.getObservation(NUMBER_OF_INDIVIDUALS_REACHED));

        } else if(subject.getObservation(TYPE_OF_INITIATIVE).equals(EDUCTION_AND_HEALTH)){
            distributionDTO.setTypeOfInitiative((String) subject.getObservation(TYPE_OF_INITIATIVE));
            distributionDTO.setSchoolAanganwadiLearningCenterName((String) subject.getObservation(SCHOOL_ANGANWADI_NAME));
            distributionDTO.setTypeOfSchool((String) subject.getObservation(TYPE_OF_SCHOOL));
        }
        else {
            distributionDTO.setTypeOfInitiative((String) subject.getObservation(TYPE_OF_INITIATIVE));
        }
        distributionDTO.setReportsCrosschecked((String) subject.getObservation(REPORTS_CROSS_CHECKED));
        distributionDTO.setRemarks((String) subject.getObservation(REMARKS));
        distributionDTO.setCreatedBy(subject.getCreatedBy());
        distributionDTO.setModifiedBy(subject.getLastModifiedBy());
        return distributionDTO;
    }

    private List<DistributionLine> fetchDistributionLineItems(Subject subject) {
        ArrayList<HashMap<String, Object>> md = (ArrayList<HashMap<String, Object>>) subject.getObservations().get(DISTRIBUTION_DETAILS);
        return md.stream().map(entry -> createDistributionLine(entry, subject)).collect(Collectors.toList());
    }

    private DistributionLine createDistributionLine(HashMap<String, Object> entry, Subject subject) {
        String implemenationInventoryId = (String) entry.get(INVENTORY_ID);
        Subject inventorySubject = avniSubjectRepository.getSubject(implemenationInventoryId);
        String inventoryExternalId = inventorySubject.getExternalId();
        String sourceId = getSourceId(subject.getUuid(), inventoryExternalId);
        String distributedTo = (String) entry.get(DISTRIBUTED_TO);
        String unit = (String) inventorySubject.getObservation(UNIT);
        int quantity = (Integer) entry.get(QUANTITY);
        return new DistributionLine(sourceId, distributedTo, inventoryExternalId, quantity, unit, inventorySubject.getUuid());
    }

    private List<DistributionActivities> fetchActivities(Subject subject) {
        String isActivityAvailableForLinking = (String) subject.getObservation(IS_ACTIVITY_AVAILABLE_FOR_LINKING);
        ArrayList<HashMap<String, Object>> md = (ArrayList<HashMap<String, Object>>) subject.getObservations().get(ACTIVITY_DETAILS);
        if (StringUtils.isBlank(isActivityAvailableForLinking)
                || isActivityAvailableForLinking.equalsIgnoreCase(NO)
                || md == null) return Collections.emptyList();
        return md.stream().map(this::createDistributionActivities)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private DistributionActivities createDistributionActivities(HashMap<String, Object> entry) {
        String activitySourceId = (String) entry.get(ACTIVITIES_DONE);
        if (activitySourceId != null) {
            return new DistributionActivities(activitySourceId);
        }
        return null;
    }

    public String getSourceId(String subjectUUID, String inventoryId) {
        return subjectUUID + DISTRIBUTION_LI_NAME_CONNECTOR + inventoryId;
    }
}
