package org.avni_integration_service.tanuh.dqs;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// What the data quality service said about a record it stored: 200 when it scored it, 422 when a field made it
// unreadable (the record is still stored, with the violations listed). rules is each rule's outcome on the record.
public record DqsResult(int status, boolean scored, List<String> violations, List<RuleOutcome> rules) {
    static final String FAIL = "fail";

    public record RuleOutcome(String ruleId, String outcome) {
    }

    // failed is every rule the record failed, failingQc those of them named in qc_fail_rules, and unreported the
    // named rules the service gave no outcome for: a misspelt id, or a rule the tenant's pack does not run on the record.
    public record QcVerdict(boolean passed, List<String> failed, List<String> failingQc, List<String> unreported) {
    }

    // The integration's verdict, not the service's: QC fails on a record the service could not read, or on a failed
    // rule named in qcFailRules. A failed rule not named there, or a rule that could not decide (indeterminate, error,
    // not applicable), does not fail the screening. Ids are matched exactly.
    public QcVerdict verdict(Set<String> qcFailRules) {
        List<String> failed = rules.stream().filter(rule -> FAIL.equals(rule.outcome())).map(RuleOutcome::ruleId).toList();
        List<String> failingQc = failed.stream().filter(qcFailRules::contains).toList();
        Set<String> reported = rules.stream().map(RuleOutcome::ruleId).collect(Collectors.toSet());
        List<String> unreported = qcFailRules.stream().filter(rule -> !reported.contains(rule)).sorted().toList();
        return new QcVerdict(scored && failingQc.isEmpty(), failed, failingQc, unreported);
    }
}
