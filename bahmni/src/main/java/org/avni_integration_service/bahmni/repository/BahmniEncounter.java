package org.avni_integration_service.bahmni.repository;

import org.avni_integration_service.bahmni.contract.OpenMRSFullEncounter;
import org.avni_integration_service.bahmni.BahmniEncounterToAvniEncounterMetaData;

import java.util.List;
import java.util.stream.Collectors;

//OpenMRS encounter can be used as general encounter, program enrolment, or program encounter
public class BahmniEncounter {
    private final OpenMRSFullEncounter openMRSEncounter;
    private final BahmniEncounterToAvniEncounterMetaData metaData;

    public BahmniEncounter(OpenMRSFullEncounter openMRSEncounter, BahmniEncounterToAvniEncounterMetaData metaData) {
        this.openMRSEncounter = openMRSEncounter;
        this.metaData = metaData;
    }

    public List<BahmniSplitEncounter> getSplitEncounters() {
        return openMRSEncounter.getMappedFormObservations(metaData::hasBahmniConceptSet).entrySet().stream()
                .map(form -> new BahmniSplitEncounter(form.getKey(), openMRSEncounter.getUuid(), openMRSEncounter.getEncounterDatetime(), form.getValue(), openMRSEncounter.isVoided()))
                .collect(Collectors.toList());
    }

    public OpenMRSFullEncounter getOpenMRSEncounter() {
        return openMRSEncounter;
    }

    public String getEncounterTypeUuid() {
        return getOpenMRSEncounter().getEncounterType().getUuid();
    }

    public String getVisitTypeUuid() {
        return openMRSEncounter.getVisitTypeUuid();
    }
}
