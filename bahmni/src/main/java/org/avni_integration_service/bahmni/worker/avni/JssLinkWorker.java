package org.avni_integration_service.bahmni.worker.avni;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.avni.domain.GeneralEncountersResponse;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.avni.repository.AvniSubjectRepository;
import org.avni_integration_service.bahmni.BahmniErrorType;
import org.avni_integration_service.avni.repository.AvniEncounterRepository;
import org.avni_integration_service.bahmni.BahmniEncounterToAvniEncounterMetaData;
import org.avni_integration_service.bahmni.BahmniMappingGroup;
import org.avni_integration_service.bahmni.BahmniMappingType;
import org.avni_integration_service.bahmni.Names;
import org.avni_integration_service.bahmni.contract.OpenMRSFullEncounter;
import org.avni_integration_service.bahmni.contract.OpenMRSObservation;
import org.avni_integration_service.bahmni.repository.BahmniEncounter;
import org.avni_integration_service.bahmni.repository.BahmniSplitEncounter;
import org.avni_integration_service.bahmni.service.BahmniEncounterService;
import org.avni_integration_service.bahmni.service.JssLinkErrorService;
import org.avni_integration_service.bahmni.service.JssPatientLinkService;
import org.avni_integration_service.bahmni.service.JssPatientLinkService.LinkResult;
import org.avni_integration_service.bahmni.worker.bahmni.atomfeedworker.PatientEncounterEventWorker;
import org.avni_integration_service.integration_data.domain.Constants;
import org.avni_integration_service.integration_data.domain.MappingMetaData;
import org.avni_integration_service.integration_data.repository.MappingMetaDataRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Links Avni individuals to Bahmni patients from their "Individual ID Update" visits, then syncs the patient's past
 * Bahmni visits into Avni once (catch-up). In dry-run mode nothing is written anywhere; the report says what would happen.
 */
@Component
public class JssLinkWorker {
    private static final Logger logger = Logger.getLogger(JssLinkWorker.class);

    /** The latest ID Update of one Avni individual, plus Bahmni IDs from their earlier ID Updates. */
    public record SubjectLink(String avniSubjectUuid, String bahmniId, Set<String> previousBahmniIds, String censusId, String idUpdateUuid) {
    }

    public static class Report {
        public final String avniSubjectUuid;
        public final String bahmniId;
        public String status;
        public String bahmniPatient;
        public int visits;
        public int labVisits;
        public int prescriptionVisits;
        public int visitsWithNothingToSync;
        public final Map<String, Integer> avniVisitsByForm = new TreeMap<>();
        public final Set<String> answersWithoutMapping = new TreeSet<>();
        public final Map<String, Integer> questionsNotSynced = new TreeMap<>();
        public final List<String> errors = new ArrayList<>();
        public JssPatientLinkService.LinkStatus linkStatus;
        public boolean linked;

        Report(String avniSubjectUuid, String bahmniId) {
            this.avniSubjectUuid = avniSubjectUuid;
            this.bahmniId = bahmniId;
        }
    }

    private final JssPatientLinkService linkService;
    private final PatientEncounterEventWorker encounterWorker;
    private final BahmniEncounterService bahmniEncounterService;
    private final AvniEncounterRepository avniEncounterRepository;
    private final MappingMetaDataRepository mappingMetaDataRepository;
    private final BahmniMappingGroup bahmniMappingGroup;
    private final BahmniMappingType bahmniMappingType;
    private final JssLinkErrorService linkErrorService;
    private final AvniSubjectRepository avniSubjectRepository;

    public JssLinkWorker(JssPatientLinkService linkService, PatientEncounterEventWorker encounterWorker, BahmniEncounterService bahmniEncounterService,
                         AvniEncounterRepository avniEncounterRepository, MappingMetaDataRepository mappingMetaDataRepository,
                         BahmniMappingGroup bahmniMappingGroup, BahmniMappingType bahmniMappingType,
                         JssLinkErrorService linkErrorService, AvniSubjectRepository avniSubjectRepository) {
        this.linkErrorService = linkErrorService;
        this.avniSubjectRepository = avniSubjectRepository;
        this.linkService = linkService;
        this.encounterWorker = encounterWorker;
        this.bahmniEncounterService = bahmniEncounterService;
        this.avniEncounterRepository = avniEncounterRepository;
        this.mappingMetaDataRepository = mappingMetaDataRepository;
        this.bahmniMappingGroup = bahmniMappingGroup;
        this.bahmniMappingType = bahmniMappingType;
    }

    /** Latest "Yes + Bahmni ID" ID Update per Avni individual, from ID Updates modified after {@code since}. */
    public List<SubjectLink> readIdUpdates(Date since) {
        Map<String, GeneralEncounter> byUuid = new LinkedHashMap<>();
        Date from = since;
        while (true) {
            GeneralEncountersResponse response = avniEncounterRepository.getGeneralEncounters(from, Names.IdUpdateEncounterType);
            GeneralEncounter[] page = response.getContent();
            int before = byUuid.size();
            Arrays.stream(page).forEach(e -> byUuid.put(e.getUuid(), e));
            if (page.length == 0 || response.getTotalPages() <= 1 || byUuid.size() == before) break;
            from = Arrays.stream(page).map(GeneralEncounter::getLastModifiedDate).max(Date::compareTo).orElse(from);
        }

        Map<String, List<GeneralEncounter>> bySubject = byUuid.values().stream()
                .filter(e -> !Boolean.TRUE.equals(e.getVoided()))
                .collect(Collectors.groupingBy(GeneralEncounter::getSubjectId, LinkedHashMap::new, Collectors.toList()));
        List<SubjectLink> links = new ArrayList<>();
        bySubject.forEach((subjectUuid, idUpdates) -> {
            idUpdates.sort(Comparator.comparing(GeneralEncounter::getEncounterDateTime, Comparator.nullsFirst(Comparator.naturalOrder()))
                    .thenComparing(GeneralEncounter::getLastModifiedDate, Comparator.nullsFirst(Comparator.naturalOrder())));
            GeneralEncounter latestIdUpdate = idUpdates.get(idUpdates.size() - 1);
            String latest = JssPatientLinkService.bahmniIdToLink(latestIdUpdate);
            if (latest == null) return;
            Set<String> previous = idUpdates.stream().map(JssPatientLinkService::bahmniIdToLink)
                    .filter(id -> id != null && !id.equals(latest)).collect(Collectors.toCollection(TreeSet::new));
            Object censusId = latestIdUpdate.getObservation(Names.IdUpdateCensusIdConcept);
            links.add(new SubjectLink(subjectUuid, latest, previous, censusId == null ? null : censusId.toString(), latestIdUpdate.getUuid()));
        });
        return links;
    }

    /**
     * Links each individual and syncs their past visits. A Bahmni ID that appears on more than one individual's latest
     * ID Update is linked for none of them. Problems are recorded for the JSSCP report (not in dry-run) and cleared once fixed.
     */
    public List<Report> processIdUpdates(List<SubjectLink> links, boolean dryRun) {
        Map<String, List<SubjectLink>> byBahmniId = links.stream().collect(Collectors.groupingBy(SubjectLink::bahmniId, LinkedHashMap::new, Collectors.toList()));
        List<Report> reports = new ArrayList<>();
        for (SubjectLink link : links) {
            List<SubjectLink> sameId = byBahmniId.get(link.bahmniId());
            if (sameId.size() > 1) {
                Report report = new Report(link.avniSubjectUuid(), link.bahmniId());
                List<String> others = sameId.stream().map(SubjectLink::avniSubjectUuid).filter(u -> !u.equals(link.avniSubjectUuid())).collect(Collectors.toList());
                report.status = "BAHMNI_ID_ON_MULTIPLE_PEOPLE (" + String.join(", ", others) + ")";
                if (!dryRun) recordError(link, BahmniErrorType.BahmniIdOnMultiplePeople,
                        String.format("Bahmni ID %s is also entered for %d other Avni individual(s) - none of them is linked until only one has it", link.bahmniId(), others.size()),
                        Map.of("otherAvniSubjects", others));
                reports.add(report);
                continue;
            }
            Report report;
            try {
                report = linkAndSync(link, dryRun);
            } catch (RuntimeException e) {
                // e.g. Bahmni unreachable - not a problem with the person's ID, so not reported to JSSCP; picked up again next run
                logger.error(String.format("Could not link %s (Avni %s)", link.bahmniId(), link.avniSubjectUuid()), e);
                report = new Report(link.avniSubjectUuid(), link.bahmniId());
                report.status = "FAILED (will retry): " + e.getMessage();
                report.errors.add(String.valueOf(e.getMessage()));
                reports.add(report);
                continue;
            }
            if (!dryRun) {
                if (report.linked) linkErrorService.clear(link.avniSubjectUuid());
                else recordError(link, report.linkStatus);
            }
            reports.add(report);
        }
        return reports;
    }

    private void recordError(SubjectLink link, JssPatientLinkService.LinkStatus status) {
        switch (status) {
            case PATIENT_NOT_FOUND -> recordError(link, BahmniErrorType.BahmniPatientNotFound,
                    String.format("No Bahmni patient has the ID %s - check the Bahmni ID on the Individual ID Update form", link.bahmniId()), Map.of());
            case MULTIPLE_PATIENTS_FOUND -> recordError(link, BahmniErrorType.MultipleBahmniPatientsWithId,
                    String.format("More than one Bahmni patient has the ID %s - needs checking in Bahmni", link.bahmniId()), Map.of());
            case LINKED_TO_ANOTHER_PERSON -> recordError(link, BahmniErrorType.BahmniPatientLinkedToAnotherPerson,
                    String.format("Bahmni patient %s is already linked to another Avni individual", link.bahmniId()), Map.of());
            default -> {
            }
        }
    }

    private void recordError(SubjectLink link, BahmniErrorType type, String message, Map<String, Object> extra) {
        Map<String, Object> details = new LinkedHashMap<>(personDetails(link.avniSubjectUuid()));
        details.put("bahmniId", link.bahmniId());
        details.put("censusId", link.censusId());
        details.put("idUpdateUuid", link.idUpdateUuid());
        details.putAll(extra);
        linkErrorService.record(link.avniSubjectUuid(), type, message, details);
    }

    // Name and location so the field team can find the individual without the Avni UUID
    private Map<String, Object> personDetails(String avniSubjectUuid) {
        Map<String, Object> details = new LinkedHashMap<>();
        try {
            Subject subject = avniSubjectRepository.getSubject(avniSubjectUuid);
            if (subject == null) return details;
            details.put("name", String.join(" ", Objects.toString(subject.getFirstName(), ""), Objects.toString(subject.getLastName(), "")).trim());
            if (subject.get("location") instanceof Map<?, ?> location) location.forEach((level, value) -> details.put(String.valueOf(level), value));
        } catch (RuntimeException e) {
            logger.warn("Could not read Avni individual " + avniSubjectUuid + " for the error report", e);
        }
        return details;
    }

    public Report linkAndSync(SubjectLink link, boolean dryRun) {
        Constants constants = encounterWorker.getConstants();
        Report report = new Report(link.avniSubjectUuid(), link.bahmniId());
        LinkResult result = linkService.link(link.avniSubjectUuid(), link.bahmniId(), constants, dryRun);
        report.status = result.status().name() + (result.otherAvniSubjectUuid() == null ? "" : " (" + result.otherAvniSubjectUuid() + ")");
        report.linkStatus = result.status();
        report.linked = result.isLinked();
        if (!result.isLinked()) return report;

        report.bahmniPatient = result.patient().getUuid();
        link.previousBahmniIds().forEach(previousId -> {
            if (linkService.unlink(link.avniSubjectUuid(), previousId, constants, dryRun))
                report.errors.add("Unlinked earlier Bahmni ID " + previousId);
        });

        // One encounter at a time: lighter on Bahmni, and one bad encounter does not stop the rest
        String patientUuid = result.patient().getUuid();
        for (String encounterUuid : withRetry(() -> bahmniEncounterService.getAllEncounterUuidsForPatient(patientUuid))) {
            try {
                OpenMRSFullEncounter encounter = withRetry(() -> bahmniEncounterService.getEncounter(encounterUuid));
                if (encounter.isVoided()) continue;
                report.visits++;
                describe(encounter, constants, report);
                // safe to retry: the sync looks up the existing Avni visit by Bahmni encounter UUID before creating one
                if (!dryRun) withRetry(() -> {
                    encounterWorker.processEncounterForLinkedPatient(encounter, link.avniSubjectUuid());
                    return null;
                });
            } catch (RuntimeException e) {
                logger.error(String.format("Failed on Bahmni encounter %s for %s", encounterUuid, link.bahmniId()), e);
                report.errors.add(encounterUuid + ": " + e.getMessage());
            }
        }
        return report;
    }

    // JSS Bahmni is slow at times; a read that times out usually works on the next try
    private static <T> T withRetry(java.util.function.Supplier<T> read) {
        for (int attempt = 1; ; attempt++) {
            try {
                return read.get();
            } catch (RuntimeException e) {
                if (attempt >= 3 || !isTimeout(e)) throw e;
                logger.warn("Bahmni read timed out, retrying (" + attempt + ")");
                try {
                    Thread.sleep(5000L * attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    private static boolean isTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause())
            if (t instanceof java.net.SocketTimeoutException || String.valueOf(t.getMessage()).contains("timed out")) return true;
        return false;
    }

    // What the visit sync would create for this encounter, and anything in it that cannot be mapped
    private void describe(OpenMRSFullEncounter encounter, Constants constants, Report report) {
        BahmniEncounterToAvniEncounterMetaData metaData = encounterWorker.getMetaData();
        BahmniEncounter bahmniEncounter = new BahmniEncounter(encounter, metaData);
        int before = report.avniVisitsByForm.values().stream().mapToInt(Integer::intValue).sum();
        if (bahmniEncounterService.isProcessableLabEncounter(bahmniEncounter, metaData, constants)) {
            report.labVisits++;
            report.avniVisitsByForm.merge(metaData.getLabEncounterTypeMapping().getAvniValue(), 1, Integer::sum);
            checkMappings(encounter.getLeafObservations(), report);
        } else {
            if (bahmniEncounterService.isProcessablePrescriptionEncounter(bahmniEncounter, constants)) {
                report.prescriptionVisits++;
                report.avniVisitsByForm.merge(metaData.getDrugOrderEncounterTypeMapping().getAvniValue(), 1, Integer::sum);
            }
            for (BahmniSplitEncounter split : bahmniEncounter.getSplitEncounters()) {
                // the sync skips a form whose answers are all unmapped (e.g. only a photo)
                if (checkMappings(split.getObservations(), report) > 0)
                    report.avniVisitsByForm.merge(metaData.getAvniMappedName(split.getFormConceptSetUuid()), 1, Integer::sum);
            }
        }
        if (report.avniVisitsByForm.values().stream().mapToInt(Integer::intValue).sum() == before) report.visitsWithNothingToSync++;
    }

    /** @return how many of the observations will be synced */
    private int checkMappings(List<OpenMRSObservation> observations, Report report) {
        int mapped = 0;
        for (OpenMRSObservation observation : observations) {
            MappingMetaData question = conceptMapping(observation.getConceptUuid());
            if (question == null) {
                report.questionsNotSynced.merge(observation.getConceptUuid(), 1, Integer::sum);
                continue;
            }
            mapped++;
            if (question.isCoded() && observation.getValue() != null && conceptMapping(observation.getValue().toString()) == null) {
                report.answersWithoutMapping.add(question.getAvniValue() + " = " + observation.getValue());
            }
        }
        return mapped;
    }

    private MappingMetaData conceptMapping(String bahmniConceptUuid) {
        return mappingMetaDataRepository.findByMappingGroupAndMappingTypeAndIntSystemValueAndIsVoidedFalse(bahmniMappingGroup.observation, bahmniMappingType.concept, bahmniConceptUuid);
    }
}
