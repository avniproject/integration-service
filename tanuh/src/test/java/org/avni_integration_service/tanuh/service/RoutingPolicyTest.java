package org.avni_integration_service.tanuh.service;

import org.avni_integration_service.tanuh.domain.ModelResult;
import org.avni_integration_service.tanuh.domain.ReviewCategory;
import org.avni_integration_service.tanuh.domain.WorkerOpinion;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RoutingPolicyTest {
    private final RoutingPolicy policy = new RoutingPolicy();

    // Rows 1 to 13 of section 3 of Tanuh's requirements, with the outcome the card sets. The edge verdict is a
    // column the policy never receives. Row 14, a model error, writes nothing and is the worker's to test.
    @ParameterizedTest(name = "row {0}: worker {1}, edge {2}, model {3}, draw {4} -> {5}")
    @CsvSource({
            "1,  SUSPICIOUS,     suspicious,     HIGH_RISK,      -,     HIGH_RISK",
            "2,  SUSPICIOUS,     suspicious,     LOW_RISK,       -,     LOW_RISK",
            "3,  SUSPICIOUS,     suspicious,     NOT_SUSPICIOUS, -,     FLW_OVERRIDE",
            "4,  SUSPICIOUS,     not suspicious, HIGH_RISK,      -,     HIGH_RISK",
            "5,  SUSPICIOUS,     not suspicious, LOW_RISK,       -,     LOW_RISK",
            "6,  SUSPICIOUS,     not suspicious, NOT_SUSPICIOUS, -,     FLW_OVERRIDE",
            "7,  NOT_SUSPICIOUS, suspicious,     HIGH_RISK,      -,     HIGH_RISK",
            "8,  NOT_SUSPICIOUS, suspicious,     LOW_RISK,       -,     LOW_RISK",
            "9,  NOT_SUSPICIOUS, suspicious,     NOT_SUSPICIOUS, false, CLOSED",
            "9,  NOT_SUSPICIOUS, suspicious,     NOT_SUSPICIOUS, true,  SAFETY_SAMPLE",
            "10, NOT_SUSPICIOUS, not suspicious, NOT_SUSPICIOUS, false, CLOSED",
            "10, NOT_SUSPICIOUS, not suspicious, NOT_SUSPICIOUS, true,  SAFETY_SAMPLE",
            "11, NOT_SUSPICIOUS, not suspicious, HIGH_RISK,      -,     HIGH_RISK",
            "12, NOT_SUSPICIOUS, not suspicious, LOW_RISK,       -,     LOW_RISK",
            "13, SUSPICIOUS,     not run,        HIGH_RISK,      -,     HIGH_RISK",
            "13, SUSPICIOUS,     not run,        LOW_RISK,       -,     LOW_RISK",
            "13, SUSPICIOUS,     not run,        NOT_SUSPICIOUS, -,     FLW_OVERRIDE",
    })
    public void routesByTheAgreedTable(int row, WorkerOpinion worker, String edgeVerdict, ModelResult.Result model, String draw, ReviewCategory expected) {
        AtomicInteger draws = new AtomicInteger();
        BooleanSupplier safetySampleDraw = () -> {
            draws.incrementAndGet();
            return Boolean.parseBoolean(draw);
        };

        assertEquals(expected, policy.route(model, worker, safetySampleDraw));
        assertEquals("-".equals(draw) ? 0 : 1, draws.get(), "the draw is asked only for a cleared case the worker did not flag");
    }
}
