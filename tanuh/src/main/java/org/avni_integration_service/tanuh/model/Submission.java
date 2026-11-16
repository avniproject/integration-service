package org.avni_integration_service.tanuh.model;

// What Tanuh's model service said about one screening. detail is the answer's body, cut to 300 characters.
public record Submission(Outcome outcome, String detail) {
    public enum Outcome { ACCEPTED, REFUSED, CONFLICT }
}
