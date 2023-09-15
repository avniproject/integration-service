package org.avni_integration_service.lahi.worker;

import com.google.cloud.bigquery.TableResult;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.avni.repository.AvniSubjectRepository;
import org.avni_integration_service.lahi.repository.StudentRepository;
import org.avni_integration_service.lahi.service.DataExtractorService;
import org.avni_integration_service.lahi.service.LahiMappingMetadataService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StudentWorker {
    public static final String BULK_FETCH_QUERY = "SELECT name, age FROM `ringed-prism-398304.covid_19_open_data.doctor` LIMIT 1000";
    private static final Logger logger = Logger.getLogger(StudentWorker.class);
    private final DataExtractorService dataExtractorService;
    private final LahiMappingMetadataService lahiMappingMetadataService;
    private final StudentRepository studentRepository;


    public StudentWorker(DataExtractorService dataExtractorService, LahiMappingMetadataService lahiMappingMetadataService, StudentRepository studentRepository) {
        this.dataExtractorService = dataExtractorService;
        this.lahiMappingMetadataService = lahiMappingMetadataService;
        this.studentRepository = studentRepository;
    }

    public void fetchDetails() throws InterruptedException {
        logger.info("fetch detail working !!!!!!!!!!");
        //TODO fetch and call the StudentRepository here
        TableResult result = dataExtractorService.queryToBigQuery(BULK_FETCH_QUERY);
        List<Subject> doctorList =  lahiMappingMetadataService.mappingMetadata(result);
        studentRepository.insert(doctorList);

    }


}
