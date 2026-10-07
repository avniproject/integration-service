package org.avni_integration_service.tanuh.domain;

public enum ReviewCategory {
    HIGH_RISK("High Risk"), LOW_RISK("Low Risk"), FLW_OVERRIDE("FLW override"), NOT_SCORED("Not scored"),
    SAFETY_SAMPLE("Safety sample"), CLOSED("Closed");

    private final String answer;

    ReviewCategory(String answer) {
        this.answer = answer;
    }

    public String getAnswer() {
        return answer;
    }
}
