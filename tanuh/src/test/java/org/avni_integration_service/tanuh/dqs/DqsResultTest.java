package org.avni_integration_service.tanuh.dqs;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DqsResultTest {
    @Test
    public void aScoredRecordNoRuleFailedPasses() {
        assertTrue(new DqsResult(200, true, List.of(), Map.of("pass", 5)).passed());
        assertTrue(new DqsResult(200, true, List.of(), Map.of("pass", 3, "indeterminate", 1, "error", 1, "not_applicable", 2)).passed());
        assertTrue(new DqsResult(200, true, List.of(), Map.of()).passed());
    }

    @Test
    public void aFailedRuleFailsTheRecord() {
        assertFalse(new DqsResult(200, true, List.of(), Map.of("pass", 5, "fail", 1)).passed());
    }

    @Test
    public void anUnreadableRecordFails() {
        assertFalse(new DqsResult(422, false, List.of("error IDENTITY_FIELD_MISSING: absent"), Map.of()).passed());
    }
}
