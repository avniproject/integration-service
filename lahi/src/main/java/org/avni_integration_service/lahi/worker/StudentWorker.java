package org.avni_integration_service.lahi.worker;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.lahi.domain.Student;
import org.avni_integration_service.lahi.domain.Students;
import org.avni_integration_service.lahi.service.*;
import org.springframework.stereotype.Component;

@Component
public class StudentWorker {
    private static final Logger logger = Logger.getLogger(StudentWorker.class);
    private final LahiStudentService lahiStudentService;
    private final StudentMapper studentMapper;
    private final AvniStudentService avniStudentService;
    private final LahiIntegrationDataService lahiIntegrationDataService;
    private final StudentErrorService studentErrorService;

    public StudentWorker(LahiStudentService lahiStudentService, StudentMapper studentMapper, AvniStudentService avniStudentService, LahiIntegrationDataService lahiIntegrationDataService, StudentErrorService studentErrorService) {
        this.lahiStudentService = lahiStudentService;
        this.studentMapper = studentMapper;
        this.avniStudentService = avniStudentService;
        this.lahiIntegrationDataService = lahiIntegrationDataService;
        this.studentErrorService = studentErrorService;
    }

    public void processStudents() {
        Students students = lahiStudentService.getStudents();
        while (students.hasNext()) {
            Student student = students.next();
            try {
                Subject subject = studentMapper.mapToSubject(student);
                avniStudentService.saveStudent(subject);
                lahiIntegrationDataService.studentProcessed(student);
            } catch (Throwable t) {
                logger.error("Error while process glific student", t);
                studentErrorService.studentProcessingError(student, t);
            }
        }
    }
}
