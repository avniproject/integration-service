package org.avni_integration_service.goonj.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;

//@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"source_id", "state", "district", "block", "localityVillageName", "tolaMohalla", "dateOfDistribution",
        "accountCode", "nameOfAccount", "typeOfCommunity", "typeOfInitiative", "photographInformation", "createdBy",
        "modifiedBy", "distributionLines", "distributedTo", "inventoryIds",  "quantity", "unit",
        "activities", "surveyedBy", "monitoredByOrDistributor", "approvedOrVerifiedBy", "teamOrExternal", "nameOfPOC",
        "pocContactNo", "reachedTo", "totalNumberOfReceivers", "groupName", "anyOtherDocumentSubmitted", "reportsCrosschecked",
        "name", "gender", "age", "fatherMotherName", "phoneNumber", "presentOccupation", "monthlyIncome", "noOfFamilyMember",
        "centreName", "IsCenteraSchoolAnganwadiLearning", "shareABriefProvidedMaterial", "howtheMaterialMakesaDifference", "materialGivenFor", "noOfIndividualReached",
        "remarks", "TypeOfSchool", "SchoolAanganwadiLearningCenterName","UndertakingFormPhotographs","ReceiverListPhotographs",
        "OtherDetails","materialProvidedAsPartOfRahat"})
public class DistributionDTO {

    @JsonProperty("sourceId")
    private String source_id;
    @JsonProperty("state")
    private String state;
    @JsonProperty("district")
    private String district;
    @JsonProperty("block")
    private String block;
    @JsonProperty("OtherBlock")
    private String otherBlock;
    @JsonProperty("OtherVillage")
    private String otherVillage;
    @JsonProperty("localityVillageName")
    private String localityVillageName;
    @JsonProperty("tolaMohalla")
    private String tolaMohalla;
    @JsonProperty("dateOfDistribution")
    private String dateOfDistribution;
    @JsonProperty("accountCode")
    private String accountCode;
    @JsonProperty("nameOfAccount")
    private String nameOfAccount;
    @JsonProperty("typeOfCommunity")
    private String typeOfCommunity;
    @JsonProperty("typeOfInitiative")
    private String typeOfInitiative;
    @JsonProperty("disasterType")
    private String disasterType;
    @JsonProperty("photographInformation")
    private String photographInformation;
    @JsonProperty("createdBy")
    private String createdBy;
    @JsonProperty("modifiedBy")
    private String modifiedBy;
    @JsonProperty("DistributionLines")
    private List<DistributionLine> distributionLines = new ArrayList<>();
    @JsonProperty("Activities")
    private List<DistributionActivities> activities = new ArrayList<>();
    @JsonProperty("surveyedBy")
    private String surveyedBy;
    @JsonProperty("monitoredByOrDistributor")
    private String monitoredByOrDistributor;
    @JsonProperty("approvedOrVerifiedBy")
    private String approvedOrVerifiedBy;
    @JsonProperty("teamOrExternal")
    private String teamOrExternal;
    @JsonProperty("nameOfPOC")
    private String nameOfPOC;
    @JsonProperty("pocContactNo")
    private String pocContactNo;
    @JsonProperty("reachedTo")
    private String reachedTo;
    @JsonProperty("totalNumberOfReceivers")
    private Integer totalNumberOfReceivers;
    @JsonProperty("groupName")
    private String groupName;
    @JsonProperty("anyOtherDocumentSubmitted")
    private String anyOtherDocumentSubmitted;
    @JsonProperty("reportsCrosschecked")
    private String reportsCrosschecked;
    @JsonProperty("name")
    private String name;
    @JsonProperty("gender")
    private String gender;
    @JsonProperty("age")
    private Integer age;
    @JsonProperty("father_MotherName")
    private String fatherMotherName;
    @JsonProperty("phoneNumber")
    private String phoneNumber;
    @JsonProperty("presentOccupation")
    private String presentOccupation;
    @JsonProperty("monthlyIncome")
    private Integer monthlyIncome;
    @JsonProperty("noOfFamilyMember")
    private Integer noOfFamilyMember;
    @JsonProperty("centreName")
    private String centreName;
    @JsonProperty("IsCenteraSchoolAnganwadiLearning")
    private boolean IsCenteraSchoolAnganwadiLearning;
    @JsonProperty("shareABriefProvidedMaterial")
    private String shareABriefProvidedMaterial;
    @JsonProperty("howtheMaterialMakesaDifference")
    private String howtheMaterialMakesaDifference;
    @JsonProperty("materialGivenFor")
    private String materialGivenFor;
    @JsonProperty("noOfIndividualReached")
    private Integer noOfIndividualReached;
    @JsonProperty("noOfFamiliesReached")
    private Integer noOfFamiliesReached;
    @JsonProperty("remarks")
    private String remarks;
    @JsonProperty("TypeOfSchool")
    private String TypeOfSchool;
    @JsonProperty("SchoolAanganwadiLearningCenterName")
    private String SchoolAanganwadiLearningCenterName;
    @JsonProperty("UndertakingFormPhotographs")
    private String undertakingFormPhotographs;
    @JsonProperty("ReceiverListPhotographs")
    private String receiverListPhotographs;
    @JsonProperty("OtherDetails")
    private String otherDetails;
    @JsonProperty("materialProvidedAsPartOfRahat")
    private String materialProvidedAsPartOfRahat;
    /**
     * No args constructor for use in serialization
     */
    public DistributionDTO() {
    }

    /**
     * @param source_id
     * @param state
     * @param district
     * @param block
     * @param otherBlock
     * @param otherVillage
     * @param localityVillageName
     * @param tolaMohalla
     * @param dateOfDistribution
     * @param accountCode
     * @param nameOfAccount
     * @param typeOfCommunity
     * @param typeOfInitiative
     * @param disasterType
     * @param photographInformation
     * @param createdBy
     * @param modifiedBy
     * @param distributionLines
     * @param activities
     * @param surveyedBy
     * @param monitoredByOrDistributor
     * @param approvedOrVerifiedBy
     * @param teamOrExternal
     * @param nameOfPOC
     * @param pocContactNo
     * @param reachedTo
     * @param totalNumberOfReceivers
     * @param groupName
     * @param anyOtherDocumentSubmitted
     * @param reportsCrosschecked
     * @param name
     * @param gender
     * @param age
     * @param fatherMotherName
     * @param phoneNumber
     * @param presentOccupation
     * @param monthlyIncome
     * @param noOfFamilyMember
     * @param centreName
     * @param shareABriefProvidedMaterial
     * @param howtheMaterialMakesaDifference
     * @param materialGivenFor
     * @param noOfIndividualReached
     * @param noOfFamiliesReached
     * @param remarks
     * @param TypeOfSchool
     * @param SchoolAanganwadiLearningCenterName
     * @param undertakingFormPhotographs
     * @param receiverListPhotographs
     * @param otherDetails
     */
    public DistributionDTO(String source_id, String state, String district, String block, String otherBlock, String otherVillage,
                           String localityVillageName, String tolaMohalla, String dateOfDistribution,
                           String accountCode, String nameOfAccount, String typeOfCommunity,
                           String typeOfInitiative, String disasterType, String photographInformation, String createdBy,
                           String modifiedBy, List<DistributionLine> distributionLines, List<DistributionActivities> activities,
                           String surveyedBy, String monitoredByOrDistributor, String approvedOrVerifiedBy,
                           String teamOrExternal, String nameOfPOC, String pocContactNo, String reachedTo,
                           Integer totalNumberOfReceivers, String groupName, String anyOtherDocumentSubmitted,
                           String reportsCrosschecked, String name, String gender, Integer age,
                           String fatherMotherName, String phoneNumber, String presentOccupation,
                           Integer monthlyIncome, Integer noOfFamilyMember, String centreName,
                           String shareABriefProvidedMaterial, String howtheMaterialMakesaDifference,
                           String materialGivenFor, Integer noOfIndividualReached, Integer noOfFamiliesReached,
                           String remarks, String TypeOfSchool, String SchoolAanganwadiLearningCenterName,
                           String undertakingFormPhotographs, String receiverListPhotographs, String otherDetails) {
        super();
        this.source_id = source_id;
        this.state = state;
        this.district = district;
        this.block = block;
        this.otherBlock = otherBlock;
        this.otherVillage = otherVillage;
        this.localityVillageName = localityVillageName;
        this.tolaMohalla = tolaMohalla;
        this.dateOfDistribution = dateOfDistribution;
        this.accountCode = accountCode;
        this.nameOfAccount = nameOfAccount;
        this.typeOfCommunity = typeOfCommunity;
        this.typeOfInitiative = typeOfInitiative;
        this.disasterType = disasterType;
        this.photographInformation = photographInformation;
        this.createdBy = createdBy;
        this.modifiedBy = modifiedBy;
        this.distributionLines = distributionLines;
        this.activities = activities;
        this.surveyedBy = surveyedBy;
        this.monitoredByOrDistributor = monitoredByOrDistributor;
        this.approvedOrVerifiedBy = approvedOrVerifiedBy;
        this.teamOrExternal = teamOrExternal;
        this.nameOfPOC = nameOfPOC;
        this.pocContactNo = pocContactNo;
        this.reachedTo = reachedTo;
        this.totalNumberOfReceivers = totalNumberOfReceivers;
        this.groupName = groupName;
        this.anyOtherDocumentSubmitted = anyOtherDocumentSubmitted;
        this.reportsCrosschecked = reportsCrosschecked;
        this.name = name;
        this.gender = gender;
        this.age = age;
        this.fatherMotherName = fatherMotherName;
        this.phoneNumber = phoneNumber;
        this.presentOccupation = presentOccupation;
        this.monthlyIncome = monthlyIncome;
        this.noOfFamilyMember = noOfFamilyMember;
        this.centreName = centreName;
        this.shareABriefProvidedMaterial = shareABriefProvidedMaterial;
        this.howtheMaterialMakesaDifference = howtheMaterialMakesaDifference;
        this.materialGivenFor = materialGivenFor;
        this.noOfFamiliesReached = noOfFamiliesReached;
        this.noOfIndividualReached = noOfIndividualReached;
        this.remarks = remarks;
        this.TypeOfSchool = TypeOfSchool;
        this.SchoolAanganwadiLearningCenterName = SchoolAanganwadiLearningCenterName;
        this.undertakingFormPhotographs = undertakingFormPhotographs;
        this.receiverListPhotographs = receiverListPhotographs;
        this.otherDetails = otherDetails;
    }

    @JsonProperty("sourceId")
    public String getSource_id() {
        return source_id;
    }

    @JsonProperty("sourceId")
    public void setSource_id(String source_id) {
        this.source_id = source_id;
    }

    @JsonProperty("state")
    public String getState() {
        return state;
    }

    @JsonProperty("state")
    public void setState(String state) {
        this.state = state;
    }

    @JsonProperty("district")
    public String getDistrict() {
        return district;
    }

    @JsonProperty("district")
    public void setDistrict(String district) {
        this.district = district;
    }

    @JsonProperty("block")
    public String getBlock() {
        return block;
    }

    @JsonProperty("block")
    public void setBlock(String block) {
        this.block = block;
    }

    @JsonProperty("OtherBlock")
    public String getOtherBlock() {
        return otherBlock;
    }

    @JsonProperty("OtherBlock")
    public void setOtherBlock(String otherBlock) {
        this.otherBlock = otherBlock;
    }

    @JsonProperty("OtherVillage")
    public String getOtherVillage() {
        return otherVillage;
    }

    @JsonProperty("OtherVillage")
    public void setOtherVillage(String otherVillage) {
        this.otherVillage = otherVillage;
    }

    @JsonProperty("localityVillageName")
    public String getLocalityVillageName() {
        return localityVillageName;
    }

    @JsonProperty("localityVillageName")
    public void setLocalityVillageName(String localityVillageName) {
        this.localityVillageName = localityVillageName;
    }

    @JsonProperty("tolaMohalla")
    public String getTolaMohalla() {
        return tolaMohalla;
    }

    @JsonProperty("tolaMohalla")
    public void setTolaMohalla(String tolaMohalla) {
        this.tolaMohalla = tolaMohalla;
    }

    @JsonProperty("dateOfDistribution")
    public String getDateOfDistribution() {
        return dateOfDistribution;
    }

    @JsonProperty("dateOfDistribution")
    public void setDateOfDistribution(String dateOfDistribution) {
        this.dateOfDistribution = dateOfDistribution;
    }

    @JsonProperty("accountCode")
    public String getAccountCode() {
        return accountCode;
    }

    @JsonProperty("accountCode")
    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    @JsonProperty("nameOfAccount")
    public String getNameOfAccount() {
        return nameOfAccount;
    }

    @JsonProperty("nameOfAccount")
    public void setNameOfAccount(String nameOfAccount) {
        this.nameOfAccount = nameOfAccount;
    }

    @JsonProperty("typeOfCommunity")
    public String getTypeOfCommunity() {
        return typeOfCommunity;
    }

    @JsonProperty("typeOfCommunity")
    public void setTypeOfCommunity(String typeOfCommunity) {
        this.typeOfCommunity = typeOfCommunity;
    }

    @JsonProperty("typeOfInitiative")
    public String getTypeOfInitiative() {
        return typeOfInitiative;
    }

    @JsonProperty("typeOfInitiative")
    public void setTypeOfInitiative(String typeOfInitiative) {
        this.typeOfInitiative = typeOfInitiative;
    }

    @JsonProperty("disasterType")
    public String getDisasterType() {
        return disasterType;
    }

    @JsonProperty("disasterType")
    public void setDisasterType(String disasterType) {
        this.disasterType = disasterType;
    }

    @JsonProperty("photographInformation")
    public String getPhotographInformation() {
        return photographInformation;
    }

    @JsonProperty("photographInformation")
    public void setPhotographInformation(String photographInformation) {
        this.photographInformation = photographInformation;
    }

    @JsonProperty("createdBy")
    public String getCreatedBy() {
        return createdBy;
    }

    @JsonProperty("createdBy")
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @JsonProperty("modifiedBy")
    public String getModifiedBy() {
        return modifiedBy;
    }

    @JsonProperty("modifiedBy")
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    @JsonProperty("DistributionLines")
    public List<DistributionLine> getDistributionLines() {
        return distributionLines;
    }

    @JsonProperty("DistributionLines")
    public void setDistributionLines(List<DistributionLine> distributionLines) {
        this.distributionLines = distributionLines;
    }

    @JsonProperty("Activities")
    public List<DistributionActivities> getActivities() {
        return activities;
    }

    @JsonProperty("Activities")
    public void setActivities(List<DistributionActivities> activities) {
        this.activities = activities;
    }

    @JsonProperty("surveyedBy")
    public String getSurveyedBy() {
        return surveyedBy;
    }

    @JsonProperty("surveyedBy")
    public void setSurveyedBy(String surveyedBy) {
        this.surveyedBy = surveyedBy;
    }

    @JsonProperty("monitoredByOrDistributor")
    public String getMonitoredByOrDistributor() {
        return monitoredByOrDistributor;
    }

    @JsonProperty("monitoredByOrDistributor")
    public void setMonitoredByOrDistributor(String monitoredByOrDistributor) {
        this.monitoredByOrDistributor = monitoredByOrDistributor;
    }

    @JsonProperty("approvedOrVerifiedBy")
    public String getApprovedOrVerifiedBy() {
        return approvedOrVerifiedBy;
    }

    @JsonProperty("approvedOrVerifiedBy")
    public void setApprovedOrVerifiedBy(String approvedOrVerifiedBy) {
        this.approvedOrVerifiedBy = approvedOrVerifiedBy;
    }

    @JsonProperty("teamOrExternal")
    public String getTeamOrExternal() {
        return teamOrExternal;
    }

    @JsonProperty("teamOrExternal")
    public void setTeamOrExternal(String teamOrExternal) {
        this.teamOrExternal = teamOrExternal;
    }

    @JsonProperty("nameOfPOC")
    public String getNameOfPOC() {
        return nameOfPOC;
    }

    @JsonProperty("nameOfPOC")
    public void setNameOfPOC(String nameOfPOC) {
        this.nameOfPOC = nameOfPOC;
    }

    @JsonProperty("pocContactNo")
    public String getPocContactNo() {
        return pocContactNo;
    }

    @JsonProperty("pocContactNo")
    public void setPocContactNo(String pocContactNo) {
        this.pocContactNo = pocContactNo;
    }

    @JsonProperty("reachedTo")
    public String getReachedTo() {
        return reachedTo;
    }

    @JsonProperty("reachedTo")
    public void setReachedTo(String reachedTo) {
        this.reachedTo = reachedTo;
    }

    @JsonProperty("totalNumberOfReceivers")
    public Integer getTotalNumberOfReceivers() {
        return totalNumberOfReceivers;
    }

    @JsonProperty("totalNumberOfReceivers")
    public void setTotalNumberOfReceivers(Integer totalNumberOfReceivers) {
        this.totalNumberOfReceivers = totalNumberOfReceivers;
    }

    @JsonProperty("groupName")
    public String getGroupName() {
        return groupName;
    }

    @JsonProperty("groupName")
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    @JsonProperty("anyOtherDocumentSubmitted")
    public String getAnyOtherDocumentSubmitted() {
        return anyOtherDocumentSubmitted;
    }

    @JsonProperty("anyOtherDocumentSubmitted")
    public void setAnyOtherDocumentSubmitted(String anyOtherDocumentSubmitted) {
        this.anyOtherDocumentSubmitted = anyOtherDocumentSubmitted;
    }

    @JsonProperty("reportsCrosschecked")
    public String getReportsCrosschecked() {
        return reportsCrosschecked;
    }

    @JsonProperty("reportsCrosschecked")
    public void setReportsCrosschecked(String reportsCrosschecked) {
        this.reportsCrosschecked = reportsCrosschecked;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("gender")
    public String getGender() {
        return gender;
    }

    @JsonProperty("gender")
    public void setGender(String gender) {
        this.gender = gender;
    }

    @JsonProperty("age")
    public Integer getAge() {
        return age;
    }

    @JsonProperty("age")
    public void setAge(Integer age) {
        this.age = age;
    }

    @JsonProperty("father_MotherName")
    public String getFatherMotherName() {
        return fatherMotherName;
    }

    @JsonProperty("father_MotherName")
    public void setFatherMotherName(String fatherMotherName) {
        this.fatherMotherName = fatherMotherName;
    }

    @JsonProperty("phoneNumber")
    public String getPhoneNumber() {
        return phoneNumber;
    }

    @JsonProperty("phoneNumber")
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    @JsonProperty("presentOccupation")
    public String getPresentOccupation() {
        return presentOccupation;
    }

    @JsonProperty("presentOccupation")
    public void setPresentOccupation(String presentOccupation) {
        this.presentOccupation = presentOccupation;
    }

    @JsonProperty("monthlyIncome")
    public Integer getMonthlyIncome() {
        return monthlyIncome;
    }

    @JsonProperty("monthlyIncome")
    public void setMonthlyIncome(Integer monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    @JsonProperty("noOfFamilyMember")
    public Integer getNoOfFamilyMember() {
        return noOfFamilyMember;
    }

    @JsonProperty("noOfFamilyMember")
    public void setNoOfFamilyMember(Integer noOfFamilyMember) {
        this.noOfFamilyMember = noOfFamilyMember;
    }

    @JsonProperty("centreName")
    public String getCentreName() {
        return centreName;
    }

    @JsonProperty("centreName")
    public void setCentreName(String centreName) {
        this.centreName = centreName;
    }

    @JsonProperty("IsCenteraSchoolAnganwadiLearning")
    public boolean getIsCenteraSchoolAnganwadiLearning() {
        return IsCenteraSchoolAnganwadiLearning;
    }

    @JsonProperty("IsCenteraSchoolAnganwadiLearning")
    public void setIsCenteraSchoolAnganwadiLearning(boolean isCenteraSchoolAnganwadiLearning) {
        IsCenteraSchoolAnganwadiLearning = isCenteraSchoolAnganwadiLearning;
    }

    @JsonProperty("shareABriefProvidedMaterial")
    public String getShareABriefProvidedMaterial() {
        return shareABriefProvidedMaterial;
    }

    @JsonProperty("shareABriefProvidedMaterial")
    public void setShareABriefProvidedMaterial(String shareABriefProvidedMaterial) {
        this.shareABriefProvidedMaterial = shareABriefProvidedMaterial;
    }

    @JsonProperty("howtheMaterialMakesaDifference")
    public String getHowtheMaterialMakesaDifference() {
        return howtheMaterialMakesaDifference;
    }

    @JsonProperty("howtheMaterialMakesaDifference")
    public void setHowtheMaterialMakesaDifference(String howtheMaterialMakesaDifference) {
        this.howtheMaterialMakesaDifference = howtheMaterialMakesaDifference;
    }

    @JsonProperty("materialGivenFor")
    public String getMaterialGivenFor() {
        return materialGivenFor;
    }

    @JsonProperty("materialGivenFor")
    public void setMaterialGivenFor(String materialGivenFor) {
        this.materialGivenFor = materialGivenFor;
    }

    @JsonProperty("noOfIndividualReached")
    public Integer getNoOfIndividualReached() {
        return noOfIndividualReached;
    }

    @JsonProperty("noOfIndividualReached")
    public void setNoOfIndividualReached(Integer noOfIndividualReached) {
        this.noOfIndividualReached = noOfIndividualReached;
    }

    @JsonProperty("noOfFamiliesReached")
    public Integer getNoOfFamiliesReached() {
        return noOfFamiliesReached;
    }

    @JsonProperty("noOfFamiliesReached")
    public void setNoOfFamiliesReached(Integer noOfFamiliesReached) {
        this.noOfFamiliesReached = noOfFamiliesReached;
    }

    @JsonProperty("remarks")
    public String getRemarks() {
        return remarks;
    }

    @JsonProperty("remarks")
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    @JsonProperty("TypeOfSchool")
    public String getTypeOfSchool() {
        return TypeOfSchool;
    }

    @JsonProperty("TypeOfSchool")
    public void setTypeOfSchool(String typeOfSchool) {
        TypeOfSchool = typeOfSchool;
    }

    @JsonProperty("SchoolAanganwadiLearningCenterName")
    public String getSchoolAanganwadiLearningCenterName() {
        return SchoolAanganwadiLearningCenterName;
    }

    @JsonProperty("SchoolAanganwadiLearningCenterName")
    public void setSchoolAanganwadiLearningCenterName(String schoolAanganwadiLearningCenterName) {
        SchoolAanganwadiLearningCenterName = schoolAanganwadiLearningCenterName;
    }

    @JsonProperty("UndertakingFormPhotographs")
    public String getUndertakingFormPhotographs() {
        return undertakingFormPhotographs;
    }

    @JsonProperty("UndertakingFormPhotographs")
    public void setUndertakingFormPhotographs(String undertakingFormPhotographs) {
        this.undertakingFormPhotographs = undertakingFormPhotographs;
    }
    @JsonProperty("ReceiverListPhotographs")
    public String getReceiverListPhotographs() {
        return receiverListPhotographs;
    }
    @JsonProperty("ReceiverListPhotographs")
    public void setReceiverListPhotographs(String receiverListPhotographs) {
        this.receiverListPhotographs = receiverListPhotographs;
    }
    @JsonProperty("OtherDetails")
    public String getOtherDetails() {
        return otherDetails;
    }
    @JsonProperty("OtherDetails")
    public void setOtherDetails(String otherDetails) {
        this.otherDetails = otherDetails;
    }

    @JsonProperty("materialProvidedAsPartOfRahat")
    public String getMaterialProvidedAsPartOfRahat() {
        return materialProvidedAsPartOfRahat;
    }

    @JsonProperty("materialProvidedAsPartOfRahat")
    public void setMaterialProvidedAsPartOfRahat(String materialProvidedAsPartOfRahat) {
        this.materialProvidedAsPartOfRahat = materialProvidedAsPartOfRahat;
    }
}
