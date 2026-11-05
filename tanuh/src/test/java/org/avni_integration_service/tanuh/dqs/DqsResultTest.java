package org.avni_integration_service.tanuh.dqs;

import org.avni_integration_service.tanuh.dqs.DqsResult.RuleOutcome;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class DqsResultTest {
    private static final Set<String> FAIL_QC = Set.of("ST-001", "SM-003");

    private static DqsResult scored(RuleOutcome... rules) {
        return new DqsResult(200, true, List.of(), List.of(rules));
    }

    @Test
    public void noFailedRulePasses() {
        assertTrue(scored(new RuleOutcome("ST-001", "pass")).passed(FAIL_QC));
        assertTrue(scored(new RuleOutcome("ST-001", "indeterminate"), new RuleOutcome("SM-003", "error"),
                new RuleOutcome("SL-004", "not_applicable")).passed(FAIL_QC), "a rule that could not decide does not fail it");
        assertTrue(scored().passed(FAIL_QC));
    }

    @Test
    public void aFailedRuleFailsQcOnlyWhenItIsOneOfTheNamedRules() {
        DqsResult result = scored(new RuleOutcome("ST-001", "pass"), new RuleOutcome("SL-001", "fail"), new RuleOutcome("SM-003", "fail"));

        assertFalse(result.passed(FAIL_QC));
        assertEquals(List.of("SL-001", "SM-003"), result.failedRules());
        assertEquals(List.of("SM-003"), result.failedRulesThatFailQc(FAIL_QC));
        assertTrue(scored(new RuleOutcome("SL-001", "fail")).passed(FAIL_QC));
    }

    @Test
    public void anUnreadableRecordFailsWhateverTheRules() {
        assertFalse(new DqsResult(422, false, List.of("error IDENTITY_FIELD_MISSING: absent"), List.of()).passed(FAIL_QC));
        assertFalse(new DqsResult(422, false, List.of(), List.of()).passed(Set.of()));
    }
}
