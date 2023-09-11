package org.avni_integration_service.lahi.worker;

import org.apache.log4j.Logger;
import org.avni_integration_service.lahi.service.DataExtractorService;
import org.springframework.stereotype.Component;

@Component
public class StudentWorker {
    private static final Logger logger = Logger.getLogger(StudentWorker.class);
    private final DataExtractorService dataExtractorService;

    public StudentWorker(DataExtractorService dataExtractorService) {
        this.dataExtractorService = dataExtractorService;
    }


    public void fetchDetails() throws InterruptedException {
        //TODO
        logger.info("fetch detail working !!!!!!!!!!");
        dataExtractorService.queryToBigQuery("SELECT name, age FROM `ringed-prism-398304.covid_19_open_data.doctor` LIMIT 1000");
    }
}
