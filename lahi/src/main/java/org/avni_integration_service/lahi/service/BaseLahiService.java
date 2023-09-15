package org.avni_integration_service.lahi.service;

import org.avni_integration_service.integration_data.repository.MappingMetaDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class BaseLahiService {
    protected static final Logger logger = LoggerFactory.getLogger(BaseLahiService.class);
    private final MappingMetaDataRepository mappingMetaDataRepository;


    protected BaseLahiService(MappingMetaDataRepository mappingMetaDataRepository) {
        this.mappingMetaDataRepository = mappingMetaDataRepository;
    }

    // TODO: 13/09/23
    /*
    protected void populateObservations(ObservationHolder observationHolder, GoonjEntity goonjEntity, String mappingGroup) {
        List<String> observationFields = goonjEntity.getObservationFields();
        for (String obsField : observationFields) {
            MappingMetaData mapping = mappingMetaDataRepository.getAvniMappingIfPresent(mappingGroup, MappingType_Obs, obsField, goonjContextProvider.get().getIntegrationSystem().getId());
            if(mapping == null) {
                logger.error("Mapping entry not found for observation field: " + obsField);
                continue;
            }
            ObsDataType dataTypeHint = mapping.getDataTypeHint();
            if (dataTypeHint == null)
                observationHolder.addObservation(mapping.getAvniValue(), goonjEntity.getValue(obsField));
            else if (dataTypeHint == ObsDataType.Coded && goonjEntity.getValue(obsField) != null) {
                MappingMetaData answerMapping = mappingMetaDataRepository.getAvniMappingIfPresent(mappingGroup, MappingType_Obs, goonjEntity.getValue(obsField).toString(), goonjContextProvider.get().getIntegrationSystem().getId());
                if(answerMapping == null) {
                    String errorMessage = "Answer Mapping entry not found for coded concept answer field: " + obsField;
                    logger.error(errorMessage);
                    throw new MappingException(errorMessage);
                }
                observationHolder.addObservation(mapping.getAvniValue(), answerMapping.getAvniValue());
            }
        }
    }*/
}
