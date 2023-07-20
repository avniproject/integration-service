package org.avni_integration_service.integration_data.repository;

import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.MappingGroup;
import org.avni_integration_service.integration_data.domain.MappingMetaData;
import org.avni_integration_service.integration_data.domain.MappingType;
import org.avni_integration_service.integration_data.domain.framework.MappingException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MappingMetaDataRepository extends BaseRepository<MappingMetaData> {
    MappingMetaData findByIdAndIntegrationSystemAndIsVoidedFalse(int id, IntegrationSystem integrationSystem);
    MappingMetaData findByUuidAndIntegrationSystem(String uuid, IntegrationSystem integrationSystem);

    MappingMetaData findByMappingGroupAndMappingTypeAndIsVoidedFalse(MappingGroup mappingGroup, MappingType mappingType);

    MappingMetaData findByMappingGroupNameAndMappingTypeNameAndIntSystemValueAndIntegrationSystemAndIsVoidedFalse(String mappingGroup, String mappingType, String intSystemValue, IntegrationSystem integrationSystem);

    MappingMetaData findByMappingGroupNameAndMappingTypeNameAndAvniValueAndIntegrationSystemAndIsVoidedFalse(String mappingGroup, String mappingType, String avniValue, IntegrationSystem integrationSystem);

    default MappingMetaData getAvniMapping(String mappingGroup, String mappingType, String intSystemValue, IntegrationSystem integrationSystem) {
        MappingMetaData mapping = this.getAvniMappingIfPresent(mappingGroup, mappingType, intSystemValue, integrationSystem);
        if (mapping == null)
            throw new MappingException(String.format("No mapping found for MappingGroup: %s, MappingType: %s, IntSystemValue: %s", mappingGroup, mappingType, intSystemValue));
        return mapping;
    }

    default MappingMetaData getAvniMappingIfPresent(String mappingGroup, String mappingType, String intSystemValue, IntegrationSystem integrationSystem) {
        return findByMappingGroupNameAndMappingTypeNameAndIntSystemValueAndIntegrationSystemAndIsVoidedFalse(mappingGroup, mappingType, intSystemValue, integrationSystem);
    }

    default MappingMetaData getIntSystemMappingIfPresent(String mappingGroup, String mappingType, String avniMapping, IntegrationSystem integrationSystem) {
        return findByMappingGroupNameAndMappingTypeNameAndAvniValueAndIntegrationSystemAndIsVoidedFalse(mappingGroup, mappingType, avniMapping, integrationSystem);
    }

    MappingMetaData findByMappingGroupAndMappingTypeAndIntSystemValueAndIsVoidedFalse(MappingGroup mappingGroup, MappingType mappingType, String intSystemValue);

    MappingMetaData findByMappingGroupAndMappingTypeAndAvniValueAndIsVoidedFalse(MappingGroup mappingGroup, MappingType mappingType, String avniValue);

    List<MappingMetaData> findAllByMappingGroupAndMappingTypeAndIsVoidedFalse(MappingGroup mappingGroup, MappingType mappingType);

    List<MappingMetaData> findAllByMappingGroupAndMappingTypeInAndIsVoidedFalse(MappingGroup mappingGroup, List<MappingType> mappingTypes);

    List<MappingMetaData> findAllByMappingGroupNameAndIntegrationSystemAndIsVoidedFalse(String mappingGroup, IntegrationSystem integrationSystem);

    List<MappingMetaData> findAllByMappingTypeAndIsVoidedFalse(MappingType mappingType);

    Page<MappingMetaData> findAllByAvniValueContainsAndIntegrationSystemAndIsVoidedFalse(String avniValue, IntegrationSystem integrationSystem, Pageable pageable);

    Page<MappingMetaData> findAllByIntSystemValueContainsAndIntegrationSystemAndIsVoidedFalse(String intSystemValue, IntegrationSystem integrationSystem, Pageable pageable);

    Page<MappingMetaData> findAllByAvniValueContainsAndIntSystemValueContainsAndIntegrationSystemAndIsVoidedFalse(String avniValue, String intSystemValue, IntegrationSystem integrationSystem, Pageable pageable);

    MappingMetaData findByMappingTypeAndIsVoidedFalse(MappingType mappingType);

    List<MappingMetaData> findAllByMappingTypeInAndAvniValueAndIsVoidedFalse(Collection<MappingType> mappingTypes, String avniValue);

    Page<MappingMetaData> findAllByIntegrationSystemAndIsVoidedFalse(IntegrationSystem currentIntegrationSystem, Pageable pageable);
}
