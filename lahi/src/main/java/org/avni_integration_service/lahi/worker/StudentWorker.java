package org.avni_integration_service.lahi.worker;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.common.MessageUnprocessableException;
import org.avni_integration_service.common.PlatformException;
import org.avni_integration_service.common.UnknownException;
import org.avni_integration_service.lahi.config.LahiErrorType;
import org.avni_integration_service.lahi.domain.Student;
import org.avni_integration_service.lahi.domain.Students;
import org.avni_integration_service.lahi.service.*;
import org.avni_integration_service.lahi.util.DateTimeUtil;
import org.springframework.stereotype.Component;

import java.util.Date;

import static org.avni_integration_service.lahi.service.LahiStudentService.STUDENT_ENTITY_TYPE;

@Component
public class StudentWorker {
    public static final boolean UPDATE_SYNC_STATUS = true;
    public static final boolean DO_NOT_UPDATE_SYNC_STATUS = false;

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
            Throwable throwable = null;
            try {
                student.validate();
                Subject subject = studentMapper.mapToSubject(student);
                avniStudentService.saveStudent(subject);
            } catch (PlatformException | UnknownException | MessageUnprocessableException e) {
                throwable = e;
            } finally {
                studentProcessed(student, throwable);
            }
        }
    }

    private void studentProcessed(Student student, Throwable throwable) {
        LahiErrorType errorType = null;
        boolean shouldUpdate = UPDATE_SYNC_STATUS;
        if (throwable != null) {
            if (throwable instanceof PlatformException) {
                logger.error("Platform level issue. Stop processing.", throwable);
                shouldUpdate = DO_NOT_UPDATE_SYNC_STATUS;
                errorType = LahiErrorType.PlatformError;
            } else if (throwable instanceof UnknownException) {
                logger.error("Unknown error. Adding to error record.", throwable);
                errorType = LahiErrorType.CommonError;
            } else if (throwable instanceof MessageUnprocessableException) {
                logger.warn(String.format("Problem with message. Continue processing. %s", throwable.getMessage()));
            }
            createOrUpdateErrorRecord(student, throwable, errorType);
        } else {
            deleteErrorRecord(student);
        }
        updateSyncStatus(student, shouldUpdate);
    }

    private void createOrUpdateErrorRecord(Student student, Throwable throwable, LahiErrorType errorType) {
        if (throwable != null && errorType != null) {
            studentErrorService.saveStudentError(student, throwable, errorType);
        }
    }

    /**
     * Successfully processed student, delete errorRecord if any
     * @param student
     */
    private void deleteErrorRecord(Student student) {
        studentErrorService.deleteStudentError(student);
    }

    private void updateSyncStatus(Student student, boolean shouldUpdate) {
        Date date = DateTimeUtil.toDate(student.getLastUpdatedAt(), DateTimeUtil.DATE_TIME);
        lahiIntegrationDataService.updateSyncStatus(STUDENT_ENTITY_TYPE, date, shouldUpdate);
    }
}
