package org.avni_integration_service.tanuh.service;

import com.bugsnag.Bugsnag;
import com.bugsnag.Severity;
import org.avni_integration_service.tanuh.config.TanuhContextProvider;
import org.springframework.stereotype.Component;

// A screening the job cannot settle on its own is reported to the error-reporting service (Bugsnag), because neither
// health check turns red for it. Each reason is its own group there, named after the organisation's integration system.
@Component
public class TanuhAlerts {
    public enum Reason {
        HELD_WITH_OTHER_PHOTOS("is held by Tanuh's model service with other photos and is parked for a person to settle with Tanuh"),
        REFUSED("was refused by Tanuh's model service and is marked not sent for good"),
        PHOTO_OUTSIDE_STORAGE("has a photo Avni will not sign, outside the organisation's media storage, so it is not sent to Tanuh's model service");

        private final String description;

        Reason(String description) {
            this.description = description;
        }
    }

    public static class ScreeningNeedsAPersonException extends RuntimeException {
        public ScreeningNeedsAPersonException(String message) {
            super(message);
        }
    }

    private final Bugsnag bugsnag;
    private final TanuhContextProvider tanuhContextProvider;

    public TanuhAlerts(Bugsnag bugsnag, TanuhContextProvider tanuhContextProvider) {
        this.bugsnag = bugsnag;
        this.tanuhContextProvider = tanuhContextProvider;
    }

    public void screeningNeedsAPerson(String uuid, Reason reason, String detail) {
        String integrationSystem = tanuhContextProvider.get().getIntegrationSystem().getName();
        bugsnag.notify(new ScreeningNeedsAPersonException(String.format("Screening %s %s: %s", uuid, reason.description, detail)),
                Severity.WARNING, report -> {
                    report.setGroupingHash("tanuh:" + reason.name());
                    report.setContext(integrationSystem);
                });
    }
}
