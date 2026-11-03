package org.avni_integration_service.tanuh.model;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.GeneralEncounter;
import org.avni_integration_service.tanuh.config.TanuhConfig;
import org.avni_integration_service.tanuh.config.TanuhContextProvider;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

// Stands in for the high-risk model service until its asynchronous interface is defined. Its mode is a setting, read
// at startup: accept takes every screening and does nothing with it, fail refuses every one.
@Component
public class StubHighRiskModelClient implements HighRiskModelClient {
    private static final Logger logger = Logger.getLogger(StubHighRiskModelClient.class);

    private final TanuhContextProvider tanuhContextProvider;

    public StubHighRiskModelClient(TanuhContextProvider tanuhContextProvider) {
        this.tanuhContextProvider = tanuhContextProvider;
    }

    @Override
    public void submit(GeneralEncounter screening, List<File> photos) throws HighRiskModelException {
        TanuhConfig config = tanuhContextProvider.get();
        if ("prod".equals(config.getEnvironment()))
            throw new HighRiskModelException("The stand-in model never takes a screening on production");
        if ("fail".equals(config.getStubMode()))
            throw new HighRiskModelException("The stand-in model is set to fail");
        logger.info(String.format("Stand-in model took screening %s with %d photos", screening.getUuid(), photos.size()));
    }
}
