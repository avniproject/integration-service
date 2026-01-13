package org.avni_integration_service.lahi.domain;

import org.avni_integration_service.glific.bigQuery.domain.FlowResult;
import org.avni_integration_service.util.ObjectJsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LahiStudentsTest {
    private FlowResult one;
    private FlowResult two;
    private FlowResult three;

    @BeforeEach
    public void setup() {
        one = new FlowResult(getJson("/flowResults/flowResultWithoutFlowCompleteFlag.json"));
        two = new FlowResult(getJson("/flowResults/flowResultWithFlowComplete.json"));
        three = new FlowResult(getJson("/flowResults/flowResultWithFlowIncomplete.json"));
    }

    @Test
    public void iterator() {
        Iterator<FlowResult> flowResults = Arrays.asList(two, two).iterator();
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
            Iterator<FlowResult> flowResults = Arrays.asList(one, two, three).iterator();
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
