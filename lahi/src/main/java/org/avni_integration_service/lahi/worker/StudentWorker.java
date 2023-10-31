package org.avni_integration_service.lahi.worker;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.integration_data.domain.AvniEntityType;
import org.avni_integration_service.lahi.domain.LahiStudent;
import org.avni_integration_service.lahi.domain.StudentErrorType;
import org.avni_integration_service.lahi.service.*;
import org.springframework.stereotype.Component;

import java.util.List;

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
        List<LahiStudent> lahiStudents = lahiStudentService.getStudents();
        lahiStudents.forEach(lahiStudent -> {
            try {
                Subject subject = studentMapper.mapToSubject(lahiStudent);
                avniStudentService.saveStudent(subject);
                lahiIntegrationDataService.studentProcessed(lahiStudent);
            } catch (Throwable t) {
                studentErrorService.errorOccurred(lahiStudent.getFlowResult(), StudentErrorType.CommonError, AvniEntityType.Subject, t.getMessage());
            }
        });
    }
}
