package org.avni_integration_service.lahi.worker;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.integration_data.domain.AvniEntityType;
import org.avni_integration_service.lahi.domain.LahiStudent;
import org.avni_integration_service.lahi.domain.StudentErrorType;
import org.avni_integration_service.lahi.repository.AvniStudentRepository;
import org.avni_integration_service.lahi.service.LahiIntegrationDataService;
import org.avni_integration_service.lahi.service.StudentErrorService;
import org.avni_integration_service.lahi.service.StudentMapper;
import org.avni_integration_service.lahi.service.StudentService;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.avni_integration_service.lahi.domain.StudentConstants.FLOWRESULT_ID;

@Component
public class StudentWorker {
    private static final Logger logger = Logger.getLogger(StudentWorker.class);
    private final StudentService studentService;
    private final StudentMapper studentMapper;
    private final AvniStudentRepository avniStudentRepository;
    private final LahiIntegrationDataService lahiIntegrationDataService;
    private final StudentErrorService studentErrorService;

    public StudentWorker(StudentService studentService, StudentMapper studentMapper, AvniStudentRepository avniStudentRepository, LahiIntegrationDataService lahiIntegrationDataService, StudentErrorService studentErrorService) {
        this.studentService = studentService;
        this.studentMapper = studentMapper;
        this.avniStudentRepository = avniStudentRepository;
        this.lahiIntegrationDataService = lahiIntegrationDataService;
        this.studentErrorService = studentErrorService;
    }

    public void processStudents() {
        List<LahiStudent> students = studentService.getStudents();
        students.forEach(student -> {
            try {
                Subject subject = student.subjectWithoutObservations();
                studentMapper.mapToSubject(subject, student);
                avniStudentRepository.addSubject(subject);
                lahiIntegrationDataService.studentProcessed(student);
            } catch (Throwable t) {
                studentErrorService.errorOccurred(student.getFlowResult(), StudentErrorType.CommonError, AvniEntityType.Subject, t.getMessage());
            }
        });
    }
}
