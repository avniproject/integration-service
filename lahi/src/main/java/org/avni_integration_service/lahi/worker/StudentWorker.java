package org.avni_integration_service.lahi.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.cloud.bigquery.*;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.lahi.config.LahiMappingDbConstants;
import org.avni_integration_service.lahi.domain.Student;
import org.avni_integration_service.lahi.domain.StudentConstants;
import org.avni_integration_service.lahi.repository.StudentRepository;
import org.avni_integration_service.lahi.service.DataExtractorService;
import org.avni_integration_service.lahi.service.LahiMappingMetadataService;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class StudentWorker {
//    public static final String BULK_FETCH_QUERY = "SELECT name, age FROM `ringed-prism-398304.covid_19_open_data.doctor` LIMIT 1000";
// TODO: 15/09/23  latter we have to remove this query
/*
   public static final String BULK_FETCH_QUERY = "SELECT fr.name, fc.id, fc.status, fc.completed_at, fc.contact_id, fc.contact_phone, fc.flow_id, fr.results, fr.updated_at, fr.inserted_at FROM `glific-lms-lahi.918956411022.flow_contexts` fc join `glific-lms-lahi.918956411022.flow_results` fr \n" +
        "on fc.id = fr.flow_context_id\n" +
        "where\n" +
        " fc.completed_at is not null and\n" +
        " fc.flow_id = '10052' and\n" +
        " fr.updated_at > @updated_at\n" +
        "order by fr.updated_at\n" +
        "limit @limit_count\n" +
        "offset @offset_count";
*/

// TODO: 21/09/23 remove query if not needed
//    public static final String BULK_FETCH_QUERY = "SELECT fr.name, fc.id, fc.status, fc.completed_at, fc.contact_id, fc.contact_phone, fc.flow_id, fr.results, fr.updated_at, fr.inserted_at FROM `glific-lms-lahi.918956411022.flow_contexts` fc join `glific-lms-lahi.918956411022.flow_results` fr \n" +
//            "on fc.id = fr.flow_context_id\n" +
//            "where\n" +
//            " fc.completed_at is not null and\n" +
//            " fc.flow_id = '10052' and\n" +
//            " fr.updated_at > @updated_at\n" +
//            "order by fr.updated_at\n" +
//            "limit @limit_count" ;

    public static final String BULK_FETCH_QUERY = "select fr.contact_phone, fr.results, s.inserted_at\n" +
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



    private static final Logger logger = Logger.getLogger(StudentWorker.class);
    public static final int LIMIT = 10;
    private final DataExtractorService dataExtractorService;
    private final LahiMappingMetadataService lahiMappingMetadataService;
    private final StudentRepository studentRepository;


    public StudentWorker(DataExtractorService dataExtractorService, LahiMappingMetadataService lahiMappingMetadataService, StudentRepository studentRepository) {
        this.dataExtractorService = dataExtractorService;
        this.lahiMappingMetadataService = lahiMappingMetadataService;
        this.studentRepository = studentRepository;
    }

    public void fetchDetails() throws InterruptedException, JsonProcessingException {
        logger.info("fetch detail starting !!!!!!!!!!");
        TableResult response   = dataExtractorService.queryWithPagination(BULK_FETCH_QUERY,"2023-07-28T12:15:40", LIMIT);
        List<Map<String,Object>> filterData = dataExtractorService.filterData(response);
        Map<String,Object> data = filterData.get(0);
        Student student = Student.from(data);
        Subject subject = student.subjectWithoutObservations();
        dataExtractorService.populateObservations(subject,student, LahiMappingDbConstants.MAPPINGGROUP_STUDENT);
        logger.info(String.format("contact_phone is %s",student.getValue(StudentConstants.STUDENT_CONTACT_NUMBER)));
        dataExtractorService.setOtherObservation(subject,student);
        // TODO: 20/09/23 need to add validation if needed
        List<Subject> subjectList = Arrays.asList(subject);
        studentRepository.insert(subjectList);


        logger.info("fetching ended");
    }


}
