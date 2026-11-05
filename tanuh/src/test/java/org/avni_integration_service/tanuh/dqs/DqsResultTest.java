package org.avni_integration_service.tanuh.dqs;

import org.avni_integration_service.tanuh.dqs.DqsResult.QcVerdict;
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
        assertTrue(scored(new RuleOutcome("ST-001", "pass"), new RuleOutcome("SM-003", "pass")).verdict(FAIL_QC).passed());
        assertTrue(scored(new RuleOutcome("ST-001", "indeterminate"), new RuleOutcome("SM-003", "error"),
                new RuleOutcome("SL-004", "not_applicable")).verdict(FAIL_QC).passed(), "a rule that could not decide does not fail it");
    }

    @Test
    public void aFailedRuleFailsQcOnlyWhenItIsOneOfTheNamedRules() {
        QcVerdict verdict = scored(new RuleOutcome("ST-001", "pass"), new RuleOutcome("SL-001", "fail"), new RuleOutcome("SM-003", "fail")).verdict(FAIL_QC);

        assertFalse(verdict.passed());
        assertEquals(List.of("SL-001", "SM-003"), verdict.failed());
        assertEquals(List.of("SM-003"), verdict.failingQc());
        assertTrue(scored(new RuleOutcome("SL-001", "fail")).verdict(FAIL_QC).passed());
    }

    @Test
    public void anUnreadableRecordFailsWhateverTheRules() {
        assertFalse(new DqsResult(422, false, List.of("error IDENTITY_FIELD_MISSING: absent"), List.of()).verdict(FAIL_QC).passed());
        assertFalse(new DqsResult(422, false, List.of(), List.of()).verdict(Set.of()).passed());
    }

    // A misspelt id, or one the pack does not run, matches nothing and so can never fail a screening.
    @Test
    public void aNamedRuleTheServiceGaveNoOutcomeForIsReportedAndIdsAreMatchedExactly() {
        QcVerdict verdict = scored(new RuleOutcome("ST-001", "fail"), new RuleOutcome("SL-001", "pass"))
                .verdict(Set.of("st-001", "SM-003", "SL-001"));

        assertTrue(verdict.passed(), "st-001 is not ST-001");
        assertEquals(List.of("SM-003", "st-001"), verdict.unreported());
        assertEquals(List.of(), scored(new RuleOutcome("ST-001", "pass"), new RuleOutcome("SM-003", "not_applicable")).verdict(FAIL_QC).unreported(),
                "a rule that did not apply was still run");
    }
}
