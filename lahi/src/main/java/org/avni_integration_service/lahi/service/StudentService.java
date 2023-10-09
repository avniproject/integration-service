package org.avni_integration_service.lahi.service;

import com.google.cloud.bigquery.TableResult;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.lahi.config.LahiMappingDbConstants;
import org.avni_integration_service.lahi.domain.Student;
import org.avni_integration_service.lahi.domain.StudentValidator;
import org.avni_integration_service.lahi.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.apache.log4j.Logger;
import java.util.List;
import java.util.Map;

import static org.avni_integration_service.lahi.domain.StudentConstants.DATE_OF_BIRTH;

@Service
public class StudentService {
    private final StudentMappingService studentMappingService;
    private final DataExtractorService dataExtractorService;
    private final StudentValidator studentValidator;
    private final StudentRepository studentRepository;
    public static final String BULK_FETCH_QUERY = "select fr.contact_phone, fr.results,fr.id as flowresult_id, s.inserted_at\n" +
            "from `glific-lms-lahi.918956411022.contacts` c, UNNEST(c.fields) AS s\n" +
            "join `glific-lms-lahi.918956411022.flow_results` fr \n" +
            "on fr.contact_phone = c.phone \n" +
            "WHERE\n" +
            "(s.label, s.value) = ('avni_reg_complete', 'Yes')\n" +
            "AND\n" +
            "fr.name = 'Avni Students Registrations Flow'\n" +
            "AND \n" +
            "s.inserted_at >= @updated_at\n" +
            "order by s.inserted_at\n" +
            "limit @limit_count\n" +
            "offset 0\n";

    public static final int LIMIT = 10;
    private static final Logger logger = Logger.getLogger(StudentService.class);


    public StudentService(StudentMappingService studentMappingService, DataExtractorService dataExtractorService, StudentValidator studentValidator, StudentRepository studentRepository) {
        this.studentMappingService = studentMappingService;
        this.dataExtractorService = dataExtractorService;
        this.studentValidator = studentValidator;
        this.studentRepository = studentRepository;
    }

    public void extractDatafromBigdata(){
        TableResult response   = null;
        try {
            response = dataExtractorService.queryWithPagination(BULK_FETCH_QUERY,"2023-07-28T12:15:40", LIMIT);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        List<Map<String,Object>> filterData = dataExtractorService.filterData(response);
        logger.info(String.format("%s Data get after fetching from glific",filterData.size()));
        spliter(filterData);
    }

    private void spliter(List<Map<String,Object>> filterData){
        logger.info("Spliting the record");
        filterData.stream().forEach(data->{
            processing(data);
        });
    }

    public void processing(Map<String,Object> data){
        boolean flag = preprocessing(data);
        if(flag){
            logger.info("preprocessing completed going for syncprocessing");
            syncprocessing(data);
        }
    }

    /*
    in preprocessing we will handle
    mandatory field check
    validation
     */
    public boolean preprocessing(Map<String,Object> data){

        try {
            if(checkAge(data)){
                return true;
            }else {
                // TODO: 09/10/23 add data to error log
                logger.info("age is not satisfied");
                return  false;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // TODO: 09/10/23 check mandatory field
    private void checkMandatory(List<Map<String,Object>> filterData){
       filterData =  filterData.stream().filter(map -> studentValidator.validateMandatoryField(map)).toList();
    }

    private boolean checkAge(Map<String,Object> data) throws Exception {
        boolean flag = studentValidator.checkAge(data.get(DATE_OF_BIRTH).toString());
        if(!flag)
            throw new Exception("Age is not valid");
        return flag;
    }


    /*
    In syncprocessing we are doing following task
    set subject field
    set observation field
    set other field
     */
    public void syncprocessing(Map<String,Object> data){
            Student student = Student.from(data);
            Subject subject = student.subjectWithoutObservations();
            studentMappingService.populateObservations(subject,student, LahiMappingDbConstants.MAPPINGGROUP_STUDENT);
            studentMappingService.setOtherObservation(subject,student);
            insert(subject);
    }

    public void insert(Subject subject){
         studentRepository.insert(subject);
    }

}
