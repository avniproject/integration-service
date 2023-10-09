package org.avni_integration_service.lahi.worker;

import org.apache.log4j.Logger;
import org.avni_integration_service.lahi.service.StudentService;
import org.springframework.stereotype.Component;

@Component
public class StudentWorker {

    private static final Logger logger = Logger.getLogger(StudentWorker.class);
    private final StudentService studentService;

    public StudentWorker(StudentService studentService) {
        this.studentService = studentService;
    }

    public void fetchDetails() throws InterruptedException {
        logger.info("fetch detail starting !!!!!!!!!!");
        studentService.extractDatafromBigdata();
        logger.info("fetching ended");
    }


}
