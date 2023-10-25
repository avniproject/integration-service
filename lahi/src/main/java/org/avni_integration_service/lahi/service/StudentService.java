package org.avni_integration_service.lahi.service;

import com.google.cloud.bigquery.TableResult;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.integration_data.domain.AvniEntityType;
import org.avni_integration_service.integration_data.domain.IntegratingEntityStatus;
import org.avni_integration_service.integration_data.repository.IntegratingEntityStatusRepository;
import org.avni_integration_service.lahi.config.LahiMappingDbConstants;
import org.avni_integration_service.lahi.domain.Student;
import org.avni_integration_service.lahi.domain.StudentErrorType;
import org.avni_integration_service.lahi.domain.StudentValidator;
import org.avni_integration_service.lahi.repository.StudentRepository;
import org.avni_integration_service.lahi.util.DateTimeUtil;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.avni_integration_service.lahi.domain.StudentConstants.*;

@Service
public class StudentService {
    public static final String ENTITYTYPE = "Student";
    private final StudentMappingService studentMappingService;
    private final DataExtractorService dataExtractorService;
    private final StudentValidator studentValidator;
    private final StudentRepository studentRepository;
    private final IntegratingEntityStatusRepository integratingEntityStatusRepository;
    private final StudentErrorService studentErrorService;

    public StudentService(StudentMappingService studentMappingService,
                          DataExtractorService dataExtractorService,
                          StudentValidator studentValidator,
                          StudentRepository studentRepository,
                          IntegratingEntityStatusRepository integratingEntityStatusRepository,
                          StudentErrorService studentErrorService) {
        this.studentMappingService = studentMappingService;
        this.dataExtractorService = dataExtractorService;
        this.studentValidator = studentValidator;
        this.studentRepository = studentRepository;
        this.integratingEntityStatusRepository = integratingEntityStatusRepository;
        this.studentErrorService = studentErrorService;
    }

    public static final String BULK_FETCH_QUERY = """
            select fr.contact_phone, fr.results,fr.id as flowresult_id, fr.inserted_at, fr.updated_at
            from `glific-lms-lahi.918956411022.flow_results` fr\s
            WHERE
            fr.name = 'Avni Students Registrations Flow'
            AND\s
            fr.updated_at >= @updated_at
            order by fr.updated_at
            limit @limit_count
            offset 0
            """;

    public static final int LIMIT = 1000;
    private static final Logger logger = Logger.getLogger(StudentService.class);

    public void extractDataFromBigdata(){
        try {
            // TODO: 10/10/23 get date avni_entity_status
            String fetchtime = getIntegratingEntityStatus().getReadUptoDateTime().toString();
            TableResult response = dataExtractorService.queryWithPagination(BULK_FETCH_QUERY,fetchtime, LIMIT);
            List<Map<String,Object>> filterData = dataExtractorService.filterData(response);
            logger.info(String.format("%s Data get after fetching from glific",filterData.size()));
            splitAndProcess(filterData);
        } catch (Throwable t) {
            //TODO invoke LAHI Failure HealthCheck
        }
    }

    private void splitAndProcess(List<Map<String,Object>> filterData){
        logger.info("Splitting the record and doing next step !!!");
        filterData.forEach(this::processing);
    }

    private void processing(Map<String,Object> data){
        try {
            logger.info("record preprocessing started");
            preprocessing(data);
            logger.info("record syncprocessing started");
            syncprocessing(data);
            logger.info("record postprocessing started");
            postprocessing();
            throw new RuntimeException("error log testing");
        } catch (Throwable t) {
            //TODO handle error by creating errorRecord
            String entity_id = data.get(FLOWRESULT_ID).toString();
            studentErrorService.errorOccurred(entity_id, StudentErrorType.CommonError, AvniEntityType.Subject,t.getMessage());
        }
    }


    /*

    in preprocessing we will handle
    mandatory field check
    validation of age

    */
    private void preprocessing(Map<String,Object> data){
        checkAge(data);
        //TODO introduce additional validations
    }

    private void checkMandatory(Map<String,Object> data){
       studentValidator.validateMandatoryField(data);
    }

    private void checkAge(Map<String,Object> data) {
        studentValidator.checkAge(data.get(DATE_OF_BIRTH).toString());
    }


/*

In syncprocessing we are doing following task
set subject field
set observation field
set other field

*/
    private void syncprocessing(Map<String,Object> data){
            Student student = Student.from(data);
            Subject subject = student.subjectWithoutObservations();
            studentMappingService.populateObservations(subject,student, LahiMappingDbConstants.MAPPINGGROUP_STUDENT);
            studentMappingService.setOtherObservation(subject,student);
            insert(subject,student);
    }

/*

In postprocessing
updating integrating_entity_status

*/
    private void postprocessing() {
//        updateIntegrationStatus(new Date());
    }

    private void updateIntegrationStatus(Date readUptoDateTime){
        IntegratingEntityStatus integratingEntityStatus = getIntegratingEntityStatus();
        // TODO: 10/10/23 getFetching record
        integratingEntityStatus.setReadUptoDateTime(readUptoDateTime);
        integratingEntityStatusRepository.save(integratingEntityStatus);
        logger.info(String.format("Updating integrating_entity_status with %s date",integratingEntityStatus.getReadUptoDateTime()));
    }


    private void insert(Subject subject, Student student){
         studentRepository.insert(subject);
         Date date = DateTimeUtil.lastUpdatedDate(student.getResponse().get(FLOW_RESULT_UPDATED_AT).toString());
         updateIntegrationStatus(date);
    }

    private IntegratingEntityStatus getIntegratingEntityStatus(){
        IntegratingEntityStatus integratingEntityStatus = integratingEntityStatusRepository.findByEntityType(ENTITYTYPE);
        if(integratingEntityStatus == null) {
            throw new RuntimeException("unable to find IntegratingEntityStatus");
        }
        return integratingEntityStatus;
    }



}
