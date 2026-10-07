package org.avni_integration_service.tanuh.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.tanuh.config.TanuhContextProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class Sampler {
    private static final Logger logger = Logger.getLogger(Sampler.class);

    private final TanuhContextProvider tanuhContextProvider;
    private final Random random;

    @Autowired
    public Sampler(TanuhContextProvider tanuhContextProvider) {
        this(tanuhContextProvider, new Random());
    }

    Sampler(TanuhContextProvider tanuhContextProvider, Random random) {
        this.tanuhContextProvider = tanuhContextProvider;
        this.random = random;
    }

    // Whether a case the model cleared goes to the physician as a safety sample. java.util.Random is safe across threads.
    public boolean draw(String screeningUuid) {
        double rate = tanuhContextProvider.get().getSafetySampleRate();
        double value = random.nextDouble();
        boolean picked = value < rate;
        logger.info(String.format("Safety sample draw for screening %s: rate %s, value %s, %s", screeningUuid, rate, value, picked ? "picked" : "not picked"));
        return picked;
    }
}
