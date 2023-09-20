package org.avni_integration_service.lahi.service;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.ObservationHolder;
import org.avni_integration_service.avni.domain.Subject;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.avni_integration_service.integration_data.repository.MappingMetaDataRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Service
public class LahiMappingMetadataService {

    private final MappingMetaDataRepository mappingMetaDataRepository;

    private final IntegrationSystemRepository integrationSystemRepository;
    private static final Logger logger = Logger.getLogger(AvniLahiErrorService.class);

    public LahiMappingMetadataService(MappingMetaDataRepository mappingMetaDataRepository, IntegrationSystemRepository integrationSystemRepository) {
        this.mappingMetaDataRepository = mappingMetaDataRepository;
        this.integrationSystemRepository = integrationSystemRepository;
    }

    //todo remove this dummy method
    public List<Subject> mappingMetadata(TableResult tableResult){
        logger.info("mappingMetadata is started !!!!!!!!!");
        List<Subject> doctorList = new LinkedList<>();
        for (FieldValueList row : tableResult.iterateAll()) {
            // String type
            String name = row.get("name").getStringValue();
            String age = row.get("age").getNumericValue().toPlainString();
            logger.info(String.format("%s is of the age %s \n", name, age));
            Subject subject = new Subject();
            subject.setSubjectType("Doctor");
            subject.setFirstName(name);
            subject.setGender("Male");
            Date date = new Date();
            subject.setRegistrationDate(date);
            subject.setAddress("Sample Location");
            doctorList.add(subject);
        }
        return doctorList;
    }

    public void populateObservation(ObservationHolder observationHolder, Map<String,Object> glificStudent){
        //TODO
    }


}
