package org.avni_integration_service.tanuh.dqs;

import java.util.List;
import java.util.Set;

// What the data quality service said about a record it stored: 200 when it scored it, 422 when a field made it
// unreadable (the record is still stored, with the violations listed). rules is each rule's outcome on the record.
public record DqsResult(int status, boolean scored, List<String> violations, List<RuleOutcome> rules) {
    static final String FAIL = "fail";

    public record RuleOutcome(String ruleId, String outcome) {
    }

    public List<String> failedRules() {
        return rules.stream().filter(rule -> FAIL.equals(rule.outcome())).map(RuleOutcome::ruleId).toList();
    }

    // The integration's verdict, not the service's: QC fails on a record the service could not read, or on a failed
    // rule named in failingRules (qc_fail_rules). A failed rule not named there, or a rule that could not decide
    // (indeterminate, error, not applicable), does not fail the screening.
    public List<String> failedRulesThatFailQc(Set<String> failingRules) {
        return failedRules().stream().filter(failingRules::contains).toList();
    }

    public boolean passed(Set<String> failingRules) {
        return scored && failedRulesThatFailQc(failingRules).isEmpty();
    }
}
