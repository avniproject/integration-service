package org.avni_integration_service.tanuh.worker;

// CHANGED: someone else changed the screening while it was being scored, so nothing was written; the newer version
// is read again (by the next main run, or by the retry job while its error record stays open).
public enum ScreeningOutcome {
    WRITTEN, SKIPPED, FAILED, CHANGED
}
