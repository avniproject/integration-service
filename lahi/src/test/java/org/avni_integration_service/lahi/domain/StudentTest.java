package org.avni_integration_service.lahi.domain;

import org.avni_integration_service.common.MessageUnprocessableException;
import org.avni_integration_service.common.PlatformException;
import org.avni_integration_service.glific.bigQuery.domain.FlowResult;
import org.avni_integration_service.util.ObjectJsonMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class StudentTest {
    private Map<String, Object> getJson(String fileName) {
        return ObjectJsonMapper.readValue(this.getClass().getResourceAsStream(fileName), Map.class);
    }

    @Test
    public void validate() throws MessageUnprocessableException, PlatformException {
        Student student = new Student(new FlowResult(getJson("/flowResults/flowResultWithFlowComplete.json")));
        student.validate();
    }

    @Test
    public void invalidBecauseIncomplete() {
        Student student = new Student(new FlowResult(getJson("/flowResults/flowResultWithFlowIncomplete.json")));
        assertThrows(MessageUnprocessableException.class, student::validate);
    }
}
