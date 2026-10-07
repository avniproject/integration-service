package org.avni_integration_service.tanuh.domain;

import java.time.Instant;

public record ModelResult(Result result, String version, Instant runTime) {
    public enum Result {
        HIGH_RISK("High Risk"), LOW_RISK("Low Risk"), NOT_SUSPICIOUS("Non Suspicious");

        private final String answer;

        Result(String answer) {
            this.answer = answer;
        }

        public String getAnswer() {
            return answer;
        }
    }
}
