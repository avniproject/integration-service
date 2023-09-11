package org.avni_integration_service.lahi.worker;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class StudentWorker {
    private static final Logger logger = Logger.getLogger(StudentWorker.class);


    public void fetchDetails(){
        //TODO
        logger.info("fetch detail working !!!!!!!!!!");
    }
}
