package org.avni_integration_service.tanuh.model;

import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.tanuh.config.TanuhConfig;
import org.avni_integration_service.tanuh.config.TanuhContextProvider;
import org.avni_integration_service.tanuh.domain.ModelResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

// Stands in for Tanuh's model until its AI partner defines the interface. Its mode is a setting, read at startup.
@Component
public class StubHighRiskModelClient implements HighRiskModelClient {
    public static final String VERSION = "stub";

    private final TanuhContextProvider tanuhContextProvider;
    private final Clock clock;

    @Autowired
    public StubHighRiskModelClient(TanuhContextProvider tanuhContextProvider) {
        this(tanuhContextProvider, Clock.systemUTC());
    }

    StubHighRiskModelClient(TanuhContextProvider tanuhContextProvider, Clock clock) {
        this.tanuhContextProvider = tanuhContextProvider;
        this.clock = clock;
    }

    @Override
    public ModelResult score(GeneralEncounter screening, List<File> photos) throws HighRiskModelException {
        TanuhConfig config = tanuhContextProvider.get();
        if ("prod".equals(config.getEnvironment()))
            throw new HighRiskModelException("The stand-in model never scores a screening on production");
        ModelResult.Result result = switch (config.getStubMode()) {
            case "fixed" -> fixedResult(config);
            case "fail" -> throw new HighRiskModelException("The stand-in model is set to fail");
            default -> ModelResult.Result.values()[Math.floorMod(screening.getUuid().hashCode(), 3)];
        };
        return new ModelResult(result, VERSION, Instant.now(clock));
    }

    private ModelResult.Result fixedResult(TanuhConfig config) throws HighRiskModelException {
        for (ModelResult.Result result : ModelResult.Result.values())
            if (result.getAnswer().equals(config.getStubFixedResult())) return result;
        throw new HighRiskModelException(String.format("model_stub_fixed_result '%s' is not one of the model's answers", config.getStubFixedResult()));
    }
}
