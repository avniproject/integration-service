package org.avni_integration_service.lahi.worker;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class LahiErrorStudentWorker {

    private static final Logger logger = Logger.getLogger(LahiErrorStudentWorker.class);

    public void processErrors(){
        //TODO
        logger.info("process error working !!!!!!!!!");
    }

}
