package org.avni_integration_service.tanuh.service;

import org.avni_integration_service.tanuh.domain.ModelResult;
import org.avni_integration_service.tanuh.domain.ReviewCategory;
import org.avni_integration_service.tanuh.domain.WorkerOpinion;
import org.springframework.stereotype.Component;

import java.util.function.BooleanSupplier;

// The agreed table (integration-service#131). The only class that changes if Tanuh changes the rules.
@Component
public class RoutingPolicy {
    public ReviewCategory route(ModelResult.Result model, WorkerOpinion worker, BooleanSupplier safetySampleDraw) {
        return switch (model) {
            case HIGH_RISK -> ReviewCategory.HIGH_RISK;
            case LOW_RISK -> ReviewCategory.LOW_RISK;
            case NOT_SUSPICIOUS -> worker == WorkerOpinion.SUSPICIOUS ? ReviewCategory.FLW_OVERRIDE
                    : safetySampleDraw.getAsBoolean() ? ReviewCategory.SAFETY_SAMPLE : ReviewCategory.CLOSED;
        };
    }
}
