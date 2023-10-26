package org.avni_integration_service.lahi.service;

import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.ObservationHolder;
import org.avni_integration_service.integration_data.domain.MappingMetaData;
import org.avni_integration_service.integration_data.domain.framework.MappingException;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.avni_integration_service.integration_data.repository.MappingMetaDataRepository;
import org.avni_integration_service.lahi.config.LahiMappingDbConstants;
import org.avni_integration_service.lahi.domain.LahiEntity;
import org.avni_integration_service.util.ObsDataType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LahiMappingMetadataService {

    private final MappingMetaDataRepository mappingMetaDataRepository;

    private final IntegrationSystemRepository integrationSystemRepository;
    private static final Logger logger = Logger.getLogger(AvniLahiErrorService.class);

    public LahiMappingMetadataService(MappingMetaDataRepository mappingMetaDataRepository, IntegrationSystemRepository integrationSystemRepository) {
        this.mappingMetaDataRepository = mappingMetaDataRepository;
        this.integrationSystemRepository = integrationSystemRepository;
    }

    public  void populateObservations(ObservationHolder observationHolder, LahiEntity lahiEntity, String mappingGroup) {
        List<String> observationFields = lahiEntity.getObservationFields();
        for (String obsField : observationFields) {
            MappingMetaData mapping = mappingMetaDataRepository.getAvniMappingIfPresent(mappingGroup, LahiMappingDbConstants.MAPPINGTYPE_OBS, obsField, 5);
            if(mapping == null) {
                logger.error("Mapping entry not found for observation field: " + obsField);
                continue;
            }
            ObsDataType dataTypeHint = mapping.getDataTypeHint();
            if (dataTypeHint == null)
                observationHolder.addObservation(mapping.getAvniValue(), lahiEntity.getValue(obsField));
            else if (dataTypeHint == ObsDataType.Coded && lahiEntity.getValue(obsField) != null) {
                MappingMetaData answerMapping = mappingMetaDataRepository.getAvniMappingIfPresent(mappingGroup, LahiMappingDbConstants.MAPPINGTYPE_OBS, lahiEntity.getValue(obsField).toString(), 5);
                if(answerMapping == null) {
                    String errorMessage = "Answer Mapping entry not found for coded concept answer field: " + obsField;
                    logger.error(errorMessage);
                    throw new MappingException(errorMessage);
                }
                observationHolder.addObservation(mapping.getAvniValue(), answerMapping.getAvniValue());
            }
        }
        // TODO: 22/09/23 Handle Other qualifications and Other qualification stream
    }

}
