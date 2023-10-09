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

}
