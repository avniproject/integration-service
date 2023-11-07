package org.avni_integration_service.common;

import org.avni_integration_service.avni.domain.ObservationHolder;
import org.avni_integration_service.integration_data.domain.MappingMetaData;
import org.avni_integration_service.integration_data.domain.framework.MappingException;
import org.avni_integration_service.integration_data.repository.MappingMetaDataRepository;
import org.avni_integration_service.util.ObsDataType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.log4j.Logger;

import java.util.Map;

@Component
public class ObservationMapper {
    private static final Logger logger = Logger.getLogger(ObservationMapper.class);
    private final MappingMetaDataRepository mappingMetaDataRepository;

    @Autowired
    public ObservationMapper(MappingMetaDataRepository mappingMetaDataRepository) {
        this.mappingMetaDataRepository = mappingMetaDataRepository;
    }

    public void mapObservations(ObservationHolder observationHolder, Map<String, Object> externalSystemObservations, String mappingGroupName, String mappingTypeName) {
        externalSystemObservations.forEach((key, value) -> {
            MappingMetaData mapping = mappingMetaDataRepository.getAvniMappingIfPresent(mappingGroupName, mappingTypeName, key);
            if (mapping == null) {
                logger.error("Mapping entry not found for observation field: " + key);
                return;
            }
            ObsDataType dataTypeHint = mapping.getDataTypeHint();
            if (dataTypeHint == null)
                observationHolder.addObservation(mapping.getAvniValue(), value);
            else if (dataTypeHint == ObsDataType.Coded && value != null) {
                MappingMetaData answerMapping = mappingMetaDataRepository.getAvniMappingIfPresent(mappingGroupName, mappingTypeName, (String) value);
                if (answerMapping == null) {
                    String errorMessage = "Answer Mapping entry not found for coded concept answer field: " + value;
                    logger.error(errorMessage);
                    throw new MappingException(errorMessage);
                }
                observationHolder.addObservation(mapping.getAvniValue(), answerMapping.getAvniValue());
            }
        });
    }
}
