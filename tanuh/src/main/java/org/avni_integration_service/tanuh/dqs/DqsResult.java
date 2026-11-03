package org.avni_integration_service.tanuh.dqs;

import java.util.List;
import java.util.Map;

// What the data quality service said about a record it stored: 200 when it scored it, 422 when a field made it
// unreadable (the record is still stored, with the violations listed). outcomes counts rule outcomes by name.
public record DqsResult(int status, boolean scored, List<String> violations, Map<String, Integer> outcomes) {
    static final String FAIL = "fail";

    // QC pass: the service could read the record and no rule failed it. A rule that could not decide (indeterminate,
    // error, not applicable) does not fail the screening.
    public boolean passed() {
        return scored && outcomes.getOrDefault(FAIL, 0) == 0;
    }
}
