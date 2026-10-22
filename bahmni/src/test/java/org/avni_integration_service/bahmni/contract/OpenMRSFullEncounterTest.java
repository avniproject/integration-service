package org.avni_integration_service.bahmni.contract;

import org.avni_integration_service.bahmni.TestUtils;
import org.avni_integration_service.bahmni.contract.OpenMRSDefaultEncounter;
import org.avni_integration_service.bahmni.contract.OpenMRSFullEncounter;
import org.avni_integration_service.bahmni.contract.OpenMRSObservation;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class OpenMRSFullEncounterTest {
    @Test
    public void getEncounter() {
        OpenMRSFullEncounter openMRSFullEncounter = TestUtils.readResource("fullEncounter.json", OpenMRSFullEncounter.class);
        assertNotNull(openMRSFullEncounter);
        List<OpenMRSObservation> leafObservations = openMRSFullEncounter.getLeafObservations();
        assertEquals(8, leafObservations.size());
        assertFalse(openMRSFullEncounter.isVoided());
    }

    @Test
    public void getEncounter2() throws URISyntaxException {
        OpenMRSFullEncounter openMRSFullEncounter = TestUtils.readResource("fullEncounter2.json", OpenMRSFullEncounter.class);
        assertNotNull(openMRSFullEncounter);

        List<String> forms = openMRSFullEncounter.getForms();
        assertEquals(2, forms.size());
        assertEquals("c36a7537-3f10-11e4-adec-0800271c1b75", forms.get(0));
        assertEquals("c393fd1d-3f10-11e4-adec-0800271c1b75", forms.get(1));

        List<OpenMRSObservation> leafObservations = openMRSFullEncounter.getLeafObservations(forms.get(0));
        assertNotEquals(0, leafObservations.size());

        leafObservations = openMRSFullEncounter.getLeafObservations(forms.get(1));
        assertNotEquals(0, leafObservations.size());
        assertTrue(openMRSFullEncounter.isVoided());
    }

    // Obstetrics (not mapped) > [P/A (mapped) > [Fundal height], Tetanus taken]; Vitals (mapped) > [Blood Pressure (mapped) > [Systolic]]
    private static final String NESTED_FORMS_ENCOUNTER = "{\"uuid\": \"enc-1\", \"obs\": [" +
            "{\"uuid\": \"o1\", \"concept\": {\"uuid\": \"obstetrics\"}, \"voided\": false, \"groupMembers\": [" +
            "  {\"uuid\": \"o2\", \"concept\": {\"uuid\": \"p-a\"}, \"voided\": false, \"groupMembers\": [" +
            "    {\"uuid\": \"o3\", \"concept\": {\"uuid\": \"fundal-height\"}, \"value\": 32, \"voided\": false}]}," +
            "  {\"uuid\": \"o4\", \"concept\": {\"uuid\": \"tetanus-taken\"}, \"value\": true, \"voided\": false}]}," +
            "{\"uuid\": \"o5\", \"concept\": {\"uuid\": \"vitals\"}, \"voided\": false, \"groupMembers\": [" +
            "  {\"uuid\": \"o6\", \"concept\": {\"uuid\": \"blood-pressure\"}, \"voided\": false, \"groupMembers\": [" +
            "    {\"uuid\": \"o7\", \"concept\": {\"uuid\": \"systolic\"}, \"value\": 120, \"voided\": false}]}]}]}";

    @Test
    public void mappedFormInsideUnmappedGroupIsFound() throws Exception {
        OpenMRSFullEncounter encounter = new ObjectMapper().readValue(NESTED_FORMS_ENCOUNTER, OpenMRSFullEncounter.class);
        Set<String> mapped = Set.of("p-a", "vitals", "blood-pressure");

        Map<String, List<OpenMRSObservation>> forms = encounter.getMappedFormObservations(mapped::contains);

        assertEquals(List.of("p-a", "vitals"), new ArrayList<>(forms.keySet()));
        assertEquals(List.of("fundal-height"), forms.get("p-a").stream().map(OpenMRSObservation::getConceptUuid).collect(Collectors.toList()));
        assertEquals(List.of("systolic"), forms.get("vitals").stream().map(OpenMRSObservation::getConceptUuid).collect(Collectors.toList()));
    }

    @Test
    public void nothingMappedGivesNoForms() throws Exception {
        OpenMRSFullEncounter encounter = new ObjectMapper().readValue(NESTED_FORMS_ENCOUNTER, OpenMRSFullEncounter.class);
        assertTrue(encounter.getMappedFormObservations(uuid -> false).isEmpty());
    }

    @Test
    public void topLevelFormsBehaveAsBefore() {
        OpenMRSFullEncounter encounter = TestUtils.readResource("fullEncounter2.json", OpenMRSFullEncounter.class);
        List<String> forms = encounter.getForms();

        Map<String, List<OpenMRSObservation>> mappedForms = encounter.getMappedFormObservations(forms::contains);

        assertEquals(forms, new ArrayList<>(mappedForms.keySet()));
        for (String form : forms)
            assertEquals(encounter.getLeafObservations(form).size(), mappedForms.get(form).size());
    }

    @Test
    public void getEncounter3() {
        OpenMRSFullEncounter openMRSFullEncounter = TestUtils.readResource("fullEncounter3.json", OpenMRSFullEncounter.class);
        assertNotNull(openMRSFullEncounter);
        List<OpenMRSObservation> leafObservations = openMRSFullEncounter.getLeafObservations();
        OpenMRSObservation openMRSObservation1 = leafObservations.stream().filter(openMRSObservation -> openMRSObservation.getConceptUuid().equals("821eb0cd-3f10-11e4-adec-0800271c1b75")).findFirst().orElse(null);
        assertNotNull(openMRSObservation1);
    }

    @Test
    public void getDrugOrders() {
        OpenMRSFullEncounter openMRSFullEncounter = TestUtils.readResource("encounterWithDrugOrders.json", OpenMRSFullEncounter.class);
        OpenMRSDefaultEncounter openMRSDefaultEncounter = TestUtils.readResource("defaultEncounterWithDrugOrders.json", OpenMRSDefaultEncounter.class);
        assertNotNull(openMRSFullEncounter);
        List<String> drugOrders = openMRSFullEncounter.getDrugOrders(openMRSDefaultEncounter);
        assertEquals(4, drugOrders.size());
        assertTrue(openMRSFullEncounter.hasDrugOrders());
    }
}
