package org.avni_integration_service.glific.bigQuery.domain;

import org.avni_integration_service.util.ObjectJsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class FlowResultTest {
    private FlowResult flowResult;

    @BeforeEach
    public void setup() {
        Map<String, Object> json = ObjectJsonMapper.readValue(
                this.getClass().getResourceAsStream("/sampleFlowResult.json"),
                Map.class);
        flowResult = new FlowResult(json);
    }

    @Test
    public void shouldGetTopLevelFields() {
        assertEquals("913652322176", flowResult.getContactPhone());
    }

    @Test
    public void shouldGetResultItemInputByKey() throws IOException {
        assertEquals("Singh", flowResult.getInput("avni_last_name"));
    }

    @Test
    public void shouldGetResultItemCategoryByKey() throws IOException {
        assertEquals("Telangana", flowResult.getCategory("avni_state"));
    }

    @Test
    public void shouldReturnNullIfNotAvailable() throws IOException {
        assertNull(flowResult.getCategory("non-existent-key"));
    }
}
