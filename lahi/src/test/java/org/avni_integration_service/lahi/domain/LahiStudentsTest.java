package org.avni_integration_service.lahi.domain;

import org.avni_integration_service.glific.bigQuery.domain.FlowResult;
import org.avni_integration_service.util.ObjectJsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LahiStudentsTest {
    private FlowResult flowResultWithoutFlowCompleteVariable;
    private FlowResult completeFlowResult;
    private FlowResult incompleteFlowResult;

    @BeforeEach
    public void setup() {
        flowResultWithoutFlowCompleteVariable = new FlowResult(getJson("/flowResults/flowResultWithoutFlowCompleteFlag.json"));
        completeFlowResult = new FlowResult(getJson("/flowResults/flowResultWithFlowComplete.json"));
        incompleteFlowResult = new FlowResult(getJson("/flowResults/flowResultWithFlowIncomplete.json"));
    }

    @Test
    public void iterator() {
        Iterator<FlowResult> flowResults = Arrays.asList(completeFlowResult, completeFlowResult).iterator();
        Students lahiStudents = new Students(flowResults);
        int countOfStudents = 0;
        while (lahiStudents.hasNext()) {
            lahiStudents.next();
            countOfStudents++;
        }
        assertEquals(2, countOfStudents);
    }

    @Test
    public void shouldWorkWellWhenThereAreNoStudents() {
        Iterator<FlowResult> flowResults = new ArrayList<FlowResult>().iterator();
        int countOfStudents = 0;
        while (flowResults.hasNext()) {
            flowResults.next();
        }
        assertEquals(0, countOfStudents);
    }

    @Test
    public void shouldThrowNoSuchElementExceptionWhenJustUsingNext() {
        assertThrows(NoSuchElementException.class, () -> {
            Iterator<FlowResult> flowResults = Arrays.asList(flowResultWithoutFlowCompleteVariable, completeFlowResult, incompleteFlowResult).iterator();
            while (true) {
                flowResults.next();
            }
        }, "NoSuchElementExceptionWhenIteratingThroughWithoutNext");

        assertThrows(NoSuchElementException.class, () -> {
            Iterator<FlowResult> flowResults = new ArrayList<FlowResult>().iterator();
            while (true) {
                flowResults.next();
            }
        }, "NoSuchElementExceptionWhenIteratingThroughWithoutNextForEmptyResults");
    }

    private Map<String, Object> getJson(String fileName) {
        return ObjectJsonMapper.readValue(this.getClass().getResourceAsStream(fileName), Map.class);
    }
}
