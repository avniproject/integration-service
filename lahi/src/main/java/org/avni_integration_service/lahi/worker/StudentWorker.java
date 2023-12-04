package org.avni_integration_service.lahi.worker;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.common.MessageUnprocessableException;
import org.avni_integration_service.common.PlatformException;
import org.avni_integration_service.common.UnknownException;
import org.avni_integration_service.integration_data.repository.ErrorRecordRepository;
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
    private final ErrorRecordRepository errorRecordRepository;

    public StudentWorker(LahiStudentService lahiStudentService, StudentMapper studentMapper, AvniStudentService avniStudentService, LahiIntegrationDataService lahiIntegrationDataService, StudentErrorService studentErrorService, ErrorRecordRepository errorRecordRepository) {
        this.lahiStudentService = lahiStudentService;
        this.studentMapper = studentMapper;
        this.avniStudentService = avniStudentService;
        this.lahiIntegrationDataService = lahiIntegrationDataService;
        this.studentErrorService = studentErrorService;
        this.errorRecordRepository = errorRecordRepository;
    }

    public void processStudents() {
        Students students = lahiStudentService.getStudents();
        while (students.hasNext()) {
            Student student = students.next();
            try {
                processStudent(student);
                lahiIntegrationDataService.updateSyncStatus(student);
            } catch (PlatformException e) {
                logger.error("Platform level issue. Adding to error record.", e);
                studentErrorService.platformError(student, e);
                lahiIntegrationDataService.updateSyncStatus(student);
            } catch (UnknownException e) {
                logger.error("Unknown error. Adding to error record.", e);
                studentErrorService.studentProcessingError(student, e);
                lahiIntegrationDataService.updateSyncStatus(student);
            } catch (MessageUnprocessableException e) {
                logger.warn(String.format("Problem with message. Continue processing. %s", e.getMessage()));
                lahiIntegrationDataService.updateSyncStatus(student);
            }
        }
    }

    private void processStudent(Student student) throws MessageUnprocessableException, PlatformException, UnknownException {
        student.validate();
        Subject subject = studentMapper.mapToSubject(student);
        avniStudentService.saveStudent(subject);
    }

    public void processErrors() {
        errorRecordRepository.getProcessableErrorRecords().forEach(errorRecord -> {
            Student student = lahiStudentService.getStudent(errorRecord.getEntityId());
            try {
                this.processStudent(student);
                studentErrorService.processed(errorRecord, true);
            } catch (MessageUnprocessableException e) {
                logger.warn(String.format("Problem with message. Continue processing. %s", e.getMessage()));
                studentErrorService.processed(errorRecord, false);
            } catch (PlatformException | UnknownException e) {
                logger.error("Platform level issue again.", e);
            }
        });
    }
}
