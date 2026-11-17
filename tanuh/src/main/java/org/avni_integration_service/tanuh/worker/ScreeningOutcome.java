package org.avni_integration_service.tanuh.worker;

// CHANGED: someone else changed the screening while it was being checked, so nothing was written; the newer version
// is read again (by the next main run, or by the retry job while its error record stays open).
// PARKED: Tanuh's model service holds this screening with other photos, so it waits in a terminal error record for a
// person.
public enum ScreeningOutcome {
    WRITTEN, SKIPPED, FAILED, CHANGED, PARKED
}
