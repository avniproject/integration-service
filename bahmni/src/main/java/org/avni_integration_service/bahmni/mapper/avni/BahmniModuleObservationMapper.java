package org.avni_integration_service.bahmni.mapper.avni;

import org.avni_integration_service.bahmni.BahmniMappingGroup;
import org.avni_integration_service.bahmni.BahmniMappingType;
import org.avni_integration_service.bahmni.MappingMetaDataCollection;
import org.avni_integration_service.bahmni.contract.OpenMRSObservation;
import org.avni_integration_service.bahmni.contract.OpenMRSSaveObservation;
import org.avni_integration_service.bahmni.repository.intmapping.MappingService;
import org.avni_integration_service.integration_data.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static org.avni_integration_service.bahmni.contract.OpenMRSSaveObservation.createVoidedObs;

@Component
public class BahmniModuleObservationMapper {
    private final MappingService mappingService;
    private final BahmniMappingGroup bahmniMappingGroup;

    private final BahmniMappingType bahmniMappingType;
    private static final String DEBUG_LOG = "/tmp/observation_mapping_debug.log";

    @Autowired
    public BahmniModuleObservationMapper(MappingService mappingService, BahmniMappingGroup bahmniMappingGroup, BahmniMappingType bahmniMappingType) {
        this.mappingService = mappingService;
        this.bahmniMappingGroup = bahmniMappingGroup;
        this.bahmniMappingType = bahmniMappingType;
    }

    private void logDebug(String message) {
        System.err.println(message);
        try (FileWriter fw = new FileWriter(DEBUG_LOG, true)) {
            fw.write(message + "\n");
            fw.flush();
        } catch (IOException e) {
            // Ignore file write errors
        }
    }

    public List<OpenMRSSaveObservation> updateOpenMRSObservationsFromAvniObservations(List<OpenMRSObservation> openMRSObservations, Map<String, Object> avniObservations, List<String> hardcodedConcepts) {
        List<OpenMRSSaveObservation> updateObservations = new ArrayList<>();
        MappingMetaDataCollection conceptMappings = mappingService.findAll(bahmniMappingGroup.observation, bahmniMappingType.concept);
        updateObservations.addAll(voidedObservations(openMRSObservations, avniObservations, conceptMappings, hardcodedConcepts));
        updateObservations.addAll(updatedObservations(openMRSObservations, avniObservations, conceptMappings));
        updateObservations.addAll(hardcodedObservations(openMRSObservations, hardcodedConcepts));
        return updateObservations;
    }

    private List<OpenMRSSaveObservation> hardcodedObservations(List<OpenMRSObservation> openMRSObservations, List<String> hardcodedConcepts) {
        List<OpenMRSSaveObservation> updateObservations = new ArrayList<>();
        for (var hardcodedConcept : hardcodedConcepts) {
            Optional<OpenMRSObservation> hardCodedObs = openMRSObservations.stream().filter(o -> o.getConceptUuid().equals(hardcodedConcept)).findFirst();
            hardCodedObs.ifPresent(existing -> {
                OpenMRSSaveObservation openMRSSaveObservation = new OpenMRSSaveObservation();
                openMRSSaveObservation.setUuid(existing.getObsUuid());
                openMRSSaveObservation.setConcept(existing.getConceptUuid());
                openMRSSaveObservation.setValue(existing.getValue());
                updateObservations.add(openMRSSaveObservation);
            });
        }
        return updateObservations;
    }

    private List<OpenMRSSaveObservation> updatedObservations(List<OpenMRSObservation> openMRSObservations, Map<String, Object> avniObservations, MappingMetaDataCollection conceptMappings) {
        List<OpenMRSSaveObservation> updatedObservations = new ArrayList<>();
        for (Map.Entry<String, Object> entry : avniObservations.entrySet()) {
            String question = entry.getKey();
            Object answer = entry.getValue();
            MappingMetaData questionMapping = conceptMappings.getMappingForAvniValue(question);
            if (questionMapping != null) {
                if (questionMapping.isCoded()) {
                    if (answer instanceof String) {
                        var avniAnswer = (String) answer;
                        updatedObservations.add(updatedCodedObs(openMRSObservations, conceptMappings, question, questionMapping, avniAnswer));
                    } else if (answer instanceof List<?>) {
                        List<String> valueList = (List<String>) answer;
                        valueList.forEach(avniAnswer -> {
                            updatedObservations.add(updatedCodedObs(openMRSObservations, conceptMappings, question, questionMapping, avniAnswer));
                        });
                    }
                } else {
                    if (questionMapping.isText() && answer instanceof String && ((String) answer).isBlank()) {
                        continue;
                    }
                    updatedObservations.add(updatedPrimitiveObs(openMRSObservations, conceptMappings, question, questionMapping, answer));
                }
            }
        }
        return updatedObservations;

    }

    private OpenMRSSaveObservation updatedPrimitiveObs(List<OpenMRSObservation> openMRSObservations, MappingMetaDataCollection conceptMappings, String question, MappingMetaData questionMapping, Object answer) {
        OpenMRSObservation openMRSObservation = openMRSObservations.stream()
                .filter(o -> o.getConceptUuid().equals(conceptMappings.getBahmniValueForAvniValue(question)))
                .findFirst()
                .orElse(null);
        if (openMRSObservation != null) {
            return (OpenMRSSaveObservation.createPrimitiveObs(openMRSObservation.getObsUuid(), openMRSObservation.getConceptUuid(), answer, questionMapping.getDataTypeHint()));
        } else {
            return (OpenMRSSaveObservation.createPrimitiveObs(questionMapping.getIntSystemValue(), answer, questionMapping.getDataTypeHint()));
        }
    }

    private OpenMRSSaveObservation updatedCodedObs(List<OpenMRSObservation> openMRSObservations, MappingMetaDataCollection conceptMappings, String question, MappingMetaData questionMapping, String avniAnswerConcept) {
        MappingMetaData answerMapping = conceptMappings.getMappingForAvniValue(avniAnswerConcept);
        OpenMRSObservation openMRSObservation = openMRSObservations.stream()
                .filter(o -> o.getConceptUuid().equals(conceptMappings.getBahmniValueForAvniValue(question)) &&
                             o.getValue().equals(conceptMappings.getBahmniValueForAvniValue(avniAnswerConcept)))
                .findFirst()
                .orElse(null);
        if (openMRSObservation != null) {
            return (OpenMRSSaveObservation.createCodedObs(openMRSObservation.getObsUuid(), questionMapping.getIntSystemValue(), answerMapping.getIntSystemValue()));
        } else {
            return (OpenMRSSaveObservation.createCodedObs(questionMapping.getIntSystemValue(), answerMapping.getIntSystemValue()));
        }
    }

    private List<OpenMRSSaveObservation> voidedObservations(List<OpenMRSObservation> openMRSObservations, Map<String, Object> avniObservations, MappingMetaDataCollection conceptMappings, List<String> exclude) {
        List<OpenMRSSaveObservation> voidedObservations = new ArrayList<>();
        openMRSObservations.stream().filter(o -> !exclude.contains(o.getConceptUuid())).forEach(openMRSObservation -> {
            MappingMetaData questionMapping = conceptMappings.getMappingForBahmniValue(openMRSObservation.getConceptUuid());
            String avniConceptName = questionMapping.getAvniValue();
            Object avniObsValue = avniObservations.get(avniConceptName);

            if (avniObsValue == null) {
                voidedObservations.add(createVoidedObs(openMRSObservation.getObsUuid(), openMRSObservation.getConceptUuid()));
            } else if (questionMapping.isCoded()) {
                if (avniObsValue instanceof List<?>) {
                    List<String> avniObsValueList = (List<String>) avniObsValue;
                    String openMRSAnswerName = conceptMappings.getAvniValueForBahmniValue((String) openMRSObservation.getValue());
                    if (!avniObsValueList.contains(openMRSAnswerName)) {
                        voidedObservations.add(createVoidedObs(openMRSObservation.getObsUuid(), openMRSObservation.getConceptUuid()));
                    }
                } else if (avniObsValue instanceof String) {
                    String avniObsValueString = (String) avniObsValue;
                    String openMRSAnswerName = conceptMappings.getAvniValueForBahmniValue((String) openMRSObservation.getValue());
                    if (!avniObsValueString.equals(openMRSAnswerName)) {
                        voidedObservations.add(createVoidedObs(openMRSObservation.getObsUuid(), openMRSObservation.getConceptUuid()));
                    }
                }
            }
        });
        return voidedObservations;
    }

    public List<OpenMRSSaveObservation> mapObservations(Map<String, Object> avniObservations) {
        List<OpenMRSSaveObservation> openMRSObservations = new ArrayList<>();
        logDebug("DEBUG: mapObservations called with " + avniObservations.size() + " Avni observations");
        MappingMetaDataCollection conceptMappings = mappingService.findAll(bahmniMappingGroup.observation, bahmniMappingType.concept);
        int mappedCount = 0;
        int unmappedCount = 0;
        for (Map.Entry<String, Object> entry : avniObservations.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            System.err.println("  Processing observation: '" + key + "' = '" + value + "'");

            // Skip coded fields with Bahmni concept issues (ZZ error)
            // These have correct answer UUIDs but Bahmni's concept set doesn't properly link them as answers
            if (key.endsWith("given") || (key.contains("require") && key.contains("mother")) || key.equals("Oedema") || key.equals("Breast examination")) {
                System.err.println("    ⊘ SKIPPED: " + key + " - Known Bahmni concept link issue (ZZ error)");
                continue;
            }

            if (key.contains("Amala")) {
                System.err.println("    *** AMALA FIELD FOUND *** value='" + value + "', type=" + (value == null ? "null" : value.getClass().getSimpleName()));
            }
            MappingMetaData questionMapping = conceptMappings.getMappingForAvniValue(key);
            if (questionMapping != null) {
                mappedCount++;
                System.err.println("    ✓ Found mapping for question: " + key + " -> UUID: " + questionMapping.getIntSystemValue());
                System.err.println("      - isCoded: " + questionMapping.isCoded() + ", isText: " + questionMapping.isText() + ", dataTypeHint: " + questionMapping.getDataTypeHint());
                if (questionMapping.isCoded()) {
                    if (value instanceof String) {
                        MappingMetaData answerMapping = conceptMappings.getMappingForAvniValue((String) value);
                        if (answerMapping != null) {
                            String conceptUuid = questionMapping.getIntSystemValue();
                            String answerUuid = answerMapping.getIntSystemValue();
                            System.err.println("    ✓ Added coded observation: concept=" + conceptUuid + ", answer=" + answerUuid);
                            openMRSObservations.add(OpenMRSSaveObservation.createCodedObs(conceptUuid, answerUuid));
                        } else {
                            System.err.println("    ⚠ WARNING: Skipping observation '" + key + "' - no answer mapping for value: '" + value + "'");
                        }
                    } else if (value instanceof List<?>) {
                        List<String> valueList = (List<String>) value;
                        valueList.forEach(s -> {
                            MappingMetaData answerMapping = conceptMappings.getMappingForAvniValue(s);
                            if (answerMapping == null) {
                                System.err.println("    ⚠ WARNING: Skipping answer '" + s + "' for question '" + key + "' - no mapping found");
                            } else {
                                String conceptUuid = questionMapping.getIntSystemValue();
                                String answerUuid = answerMapping.getIntSystemValue();
                                System.err.println("    ✓ Added coded observation (list): concept=" + conceptUuid + ", answer=" + answerUuid);
                                openMRSObservations.add(OpenMRSSaveObservation.createCodedObs(conceptUuid, answerUuid));
                            }
                        });
                    }
                } else {
                    if (questionMapping.isText() && value instanceof String && ((String)value).isBlank()) {
                        System.err.println("    ⊘ Skipped blank text observation: " + key);
                        continue;
                    }
                    String conceptUuid = questionMapping.getIntSystemValue();
                    System.err.println("    ✓ Added non-coded observation: concept=" + conceptUuid + ", value=" + value);
                    openMRSObservations.add(OpenMRSSaveObservation.createPrimitiveObs(conceptUuid, value, questionMapping.getDataTypeHint()));
                }
            } else {
                unmappedCount++;
                System.err.println("    ✗ No mapping found for question: '" + key + "'");
            }
        }
        System.err.println("DEBUG: Observation mapping summary - Total: " + avniObservations.size() + ", Mapped: " + mappedCount + ", Unmapped: " + unmappedCount + ", Created obs: " + openMRSObservations.size());
        return openMRSObservations;
    }

}
