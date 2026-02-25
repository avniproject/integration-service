package org.avni_integration_service.bahmni.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.bahmni.BahmniMappingGroup;
import org.avni_integration_service.bahmni.BahmniMappingType;
import org.avni_integration_service.bahmni.ConstantKey;
import org.avni_integration_service.bahmni.contract.*;
import org.avni_integration_service.avni.domain.Enrolment;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.bahmni.repository.intmapping.MappingService;
import org.avni_integration_service.integration_data.domain.*;
import org.avni_integration_service.integration_data.repository.ConstantsRepository;
import org.avni_integration_service.bahmni.repository.OpenMRSVisitRepository;
import org.avni_integration_service.util.FormatAndParseUtil;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VisitService {
    private final ConstantsRepository constantsRepository;
    private final OpenMRSVisitRepository openMRSVisitRepository;
    private final MappingService mappingService;
    private final BahmniMappingGroup bahmniMappingGroup;
    private final BahmniMappingType bahmniMappingType;
    private static final Logger logger = Logger.getLogger(VisitService.class);

    public VisitService(ConstantsRepository constantsRepository, OpenMRSVisitRepository openMRSVisitRepository,
                        MappingService mappingService, BahmniMappingGroup bahmniMappingGroup, BahmniMappingType bahmniMappingType) {
        this.constantsRepository = constantsRepository;
        this.openMRSVisitRepository = openMRSVisitRepository;
        this.mappingService = mappingService;
        this.bahmniMappingGroup = bahmniMappingGroup;
        this.bahmniMappingType = bahmniMappingType;
    }

    public OpenMRSVisit getAvniRegistrationVisit(String patientUuid) {
        Constants allConstants = constantsRepository.findAllConstants();
        String locationUuid = allConstants.getValue(ConstantKey.IntegrationBahmniLocation.name());
        String visitTypeUuid = allConstants.getValue(ConstantKey.IntegrationBahmniVisitType.name());
        return openMRSVisitRepository.getVisit(patientUuid, locationUuid, visitTypeUuid);
    }

    private OpenMRSVisit getAvniRegistrationVisit(String patientUuid, Enrolment enrolment, String visitTypeUuid) {
        var avniUuidVisitAttributeTypeUuid = mappingService.getBahmniValue(bahmniMappingGroup.common,
                bahmniMappingType.avniUUIDVisitAttributeType);
        String locationUuid = constantsRepository.findAllConstants().getValue(ConstantKey.IntegrationBahmniLocation.name());
        var visits = openMRSVisitRepository.getVisits(patientUuid, locationUuid, visitTypeUuid);
        return visits.stream()
                .filter(visit -> matchesEnrolmentId(visit, enrolment, avniUuidVisitAttributeTypeUuid))
                .findFirst().orElse(null);
    }

    private OpenMRSVisit createVisit(OpenMRSPatient patient, Subject subject) {
        String location = constantsRepository.findAllConstants().getValue(ConstantKey.IntegrationBahmniLocation.name());
        String visitType = constantsRepository.findAllConstants().getValue(ConstantKey.IntegrationBahmniVisitType.name());
        return createVisit(patient, location, visitType, visitAttributes(subject));
    }

    private OpenMRSVisit createVisit(OpenMRSPatient patient, Enrolment enrolment) {
        String location = constantsRepository.findAllConstants().getValue(ConstantKey.IntegrationBahmniLocation.name());
        String programName = enrolment.getProgram();
        String visitType = mappingService.getBahmniValue(bahmniMappingGroup.programEnrolment, bahmniMappingType.communityEnrolmentVisitType, programName);

        String debugMsg = String.format("VisitService.createVisit - Enrolment: %s, Program: '%s', Location: %s, VisitType: %s (NULL=%s), EnrolmentDate: %s",
            enrolment.getUuid(), programName, location, visitType, (visitType == null), enrolment.getEnrolmentDateTime());
        logger.info(debugMsg);
        System.err.println(debugMsg);

        return createVisit(patient, location, visitType, visitAttributes(enrolment), enrolment);
    }

    private OpenMRSVisit createVisit(OpenMRSPatient patient, String location, String visitType, List<OpenMRSSaveVisitAttribute> visitAttributes, Enrolment enrolment) {
        logger.debug("=== VisitService.createVisit START (with Enrolment) ===");
        logger.debug("Patient UUID: " + patient.getUuid());
        logger.debug("Location UUID: " + location);
        logger.debug("Visit Type UUID: " + visitType);
        logger.debug("Visit Attributes count: " + (visitAttributes != null ? visitAttributes.size() : 0));
        logger.debug("Enrolment Date (for visit start date): " + enrolment.getEnrolmentDateTime());

        OpenMRSSaveVisit openMRSSaveVisit = new OpenMRSSaveVisit();
        openMRSSaveVisit.setLocation(location);
        openMRSSaveVisit.setVisitType(visitType);
        openMRSSaveVisit.setPatient(patient.getUuid());
        String startDatetime = FormatAndParseUtil.toISODateString(enrolment.getEnrolmentDateTime());
        openMRSSaveVisit.setStartDatetime(startDatetime);
        openMRSSaveVisit.setAttributes(visitAttributes);

        logger.debug("About to POST visit to Bahmni with start date: " + startDatetime);
        OpenMRSVisit visit = openMRSVisitRepository.createVisit(openMRSSaveVisit);
        logger.debug("✓ Created visit UUID: " + (visit != null ? visit.getUuid() : "null"));
        logger.debug("=== VisitService.createVisit END ===");
        return visit;
    }

    private OpenMRSVisit createVisit(OpenMRSPatient patient, String location, String visitType, List<OpenMRSSaveVisitAttribute> visitAttributes) {
        logger.debug("=== VisitService.createVisit START ===");
        logger.debug("Patient UUID: " + patient.getUuid());
        logger.debug("Location UUID: " + location);
        logger.debug("Visit Type UUID: " + visitType);
        logger.debug("Visit Attributes count: " + (visitAttributes != null ? visitAttributes.size() : 0));

        OpenMRSSaveVisit openMRSSaveVisit = new OpenMRSSaveVisit();
        openMRSSaveVisit.setLocation(location);
        openMRSSaveVisit.setVisitType(visitType);
        openMRSSaveVisit.setPatient(patient.getUuid());
        String startDatetime = FormatAndParseUtil.toISODateString(
                FormatAndParseUtil.fromIsoDateString(patient.getAuditInfo().getDateCreated()));
        openMRSSaveVisit.setStartDatetime(startDatetime);
        openMRSSaveVisit.setAttributes(visitAttributes);

        logger.debug("About to POST visit to Bahmni...");
        OpenMRSVisit visit = openMRSVisitRepository.createVisit(openMRSSaveVisit);
        logger.debug("✓ Created visit UUID: " + (visit != null ? visit.getUuid() : "null"));
        logger.debug("=== VisitService.createVisit END ===");
        return visit;
    }

    private List<OpenMRSSaveVisitAttribute> visitAttributes(Subject subject) {
        String avniIdAttributeType = mappingService.getBahmniValue(bahmniMappingGroup.common,
                bahmniMappingType.avniUUIDVisitAttributeType);
        var avniIdAttribute = new OpenMRSSaveVisitAttribute();
        avniIdAttribute.setAttributeType(avniIdAttributeType);
        avniIdAttribute.setValue(subject.getUuid());

        String avniEventDateAttributeType = mappingService.getBahmniValue(bahmniMappingGroup.common,
                bahmniMappingType.avniEventDateVisitAttributeType);
        var eventDateAttribute = new OpenMRSSaveVisitAttribute();
        eventDateAttribute.setAttributeType(avniEventDateAttributeType);
        eventDateAttribute.setValue(FormatAndParseUtil.toISODateString(subject.getRegistrationDate()));

        return List.of(avniIdAttribute, eventDateAttribute);
    }

    private List<OpenMRSSaveVisitAttribute> visitAttributes(Enrolment enrolment) {
        String avniIdAttributeType = mappingService.getBahmniValue(bahmniMappingGroup.common,
                bahmniMappingType.avniUUIDVisitAttributeType);
        var avniIdAttribute = new OpenMRSSaveVisitAttribute();
        avniIdAttribute.setAttributeType(avniIdAttributeType);
        avniIdAttribute.setValue(enrolment.getUuid());

        String avniEventDateAttributeType = mappingService.getBahmniValue(bahmniMappingGroup.common,
                bahmniMappingType.avniEventDateVisitAttributeType);
        var eventDateAttribute = new OpenMRSSaveVisitAttribute();
        eventDateAttribute.setAttributeType(avniEventDateAttributeType);
        eventDateAttribute.setValue(FormatAndParseUtil.toISODateString(enrolment.getEnrolmentDateTime()));

        return List.of(avniIdAttribute, eventDateAttribute);
    }

    public OpenMRSVisit getOrCreateVisit(OpenMRSPatient patient, Subject subject) {
        var visit = getAvniRegistrationVisit(patient.getUuid());
        if (visit == null) {
            return createVisit(patient, subject);
        }
        logger.debug("Retrieved existing visit with uuid %s".formatted(visit.getUuid()));
        return visit;
    }

    public OpenMRSVisit getOrCreateVisit(OpenMRSPatient patient, Enrolment enrolment) {
        var visitTypeUuid = mappingService.getBahmniValue(bahmniMappingGroup.programEnrolment,
                bahmniMappingType.communityEnrolmentVisitType,
                enrolment.getProgram());
        var visit = getAvniRegistrationVisit(patient.getUuid(), enrolment, visitTypeUuid);
        if (visit == null) {
            return createVisit(patient, enrolment);
        }
        logger.debug("Retrieved existing visit with uuid %s".formatted(visit.getUuid()));
        return visit;
    }

    public OpenMRSVisit getOrCreateVisit(OpenMRSPatient patient, Enrolment enrolment, java.util.Date encounterDateTime) {
        var visitTypeUuid = mappingService.getBahmniValue(bahmniMappingGroup.programEnrolment,
                bahmniMappingType.communityEnrolmentVisitType,
                enrolment.getProgram());
        var visit = getAvniRegistrationVisit(patient.getUuid(), enrolment, visitTypeUuid);
        if (visit == null) {
            return createVisitWithEncounterDate(patient, enrolment, encounterDateTime);
        }

        // Check if encounter date falls within visit date range
        if (!isEncounterDateWithinVisitRange(visit, encounterDateTime)) {
            logger.debug("Encounter date %s is outside visit date range (start: %s). Creating new visit.".formatted(
                    encounterDateTime, visit.getStartDatetime()));
            return createVisitWithEncounterDate(patient, enrolment, encounterDateTime);
        }

        logger.debug("Retrieved existing visit with uuid %s".formatted(visit.getUuid()));
        return visit;
    }

    private OpenMRSVisit createVisitWithEncounterDate(OpenMRSPatient patient, Enrolment enrolment, java.util.Date encounterDateTime) {
        String location = constantsRepository.findAllConstants().getValue(ConstantKey.IntegrationBahmniLocation.name());
        String visitType = mappingService.getBahmniValue(bahmniMappingGroup.programEnrolment, bahmniMappingType.communityEnrolmentVisitType, enrolment.getProgram());

        logger.debug("=== VisitService.createVisitWithEncounterDate START ===");
        logger.debug("Patient UUID: " + patient.getUuid());
        logger.debug("Using ENCOUNTER date instead of enrolment date: " + encounterDateTime);

        OpenMRSSaveVisit openMRSSaveVisit = new OpenMRSSaveVisit();
        openMRSSaveVisit.setLocation(location);
        openMRSSaveVisit.setVisitType(visitType);
        openMRSSaveVisit.setPatient(patient.getUuid());
        String startDatetime = FormatAndParseUtil.toISODateString(encounterDateTime);
        openMRSSaveVisit.setStartDatetime(startDatetime);
        openMRSSaveVisit.setAttributes(visitAttributes(enrolment));

        logger.debug("About to POST visit to Bahmni with encounter date: " + startDatetime);
        OpenMRSVisit visit = openMRSVisitRepository.createVisit(openMRSSaveVisit);
        logger.debug("✓ Created visit UUID: " + (visit != null ? visit.getUuid() : "null"));
        logger.debug("=== VisitService.createVisitWithEncounterDate END ===");
        return visit;
    }

    private boolean matchesEnrolmentId(OpenMRSVisit visit, Enrolment enrolment, String avniUuidVisitAttributeTypeUuid) {
        return visit.getAttributes().stream().anyMatch(visitAttribute ->
                visitAttribute.getAttributeType().getUuid().equals(avniUuidVisitAttributeTypeUuid)
                && visitAttribute.getValue().equals(enrolment.getUuid()));
    }

    private boolean isEncounterDateWithinVisitRange(OpenMRSVisit visit, java.util.Date encounterDateTime) {
        if (visit == null || encounterDateTime == null) {
            return true;
        }

        java.util.Date visitStart = visit.getStartDatetime();

        // If encounter is before visit start, it's outside the range
        if (visitStart != null && encounterDateTime.before(visitStart)) {
            return false;
        }

        // If encounter is significantly after visit start (more than a reasonable timeframe),
        // treat as outside range to handle cases where old visits are being reused
        if (visitStart != null) {
            long daysDifference = (encounterDateTime.getTime() - visitStart.getTime()) / (1000 * 60 * 60 * 24);
            // If more than 365 days apart, create a new visit
            if (daysDifference > 365) {
                return false;
            }
        }

        return true;
    }

    public void voidVisit(Enrolment enrolment, OpenMRSFullEncounter communityEnrolmentEncounter) {
        Constants allConstants = constantsRepository.findAllConstants();
        String locationUuid = allConstants.getValue(ConstantKey.IntegrationBahmniLocation.name());
        String visitType = mappingService.getBahmniValue(bahmniMappingGroup.programEnrolment, bahmniMappingType.communityEnrolmentVisitType, enrolment.getProgram());
        OpenMRSVisit visit = openMRSVisitRepository.getVisit(communityEnrolmentEncounter.getPatient().getUuid(), locationUuid, visitType);
        openMRSVisitRepository.deleteVisit(visit.getUuid());
    }
}
