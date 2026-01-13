package org.avni_integration_service.integration_data.repository;

import org.avni_integration_service.integration_data.context.IntegrationContext;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.MappingGroup;
import org.avni_integration_service.integration_data.domain.MappingMetaData;
import org.avni_integration_service.integration_data.domain.MappingType;
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

    MappingMetaData findByMappingGroupNameAndMappingTypeNameAndIntSystemValueAndIntegrationSystem(String mappingGroup, String mappingType, String intSystemValue, IntegrationSystem integrationSystem);
    MappingMetaData findByMappingGroupNameAndMappingTypeNameAndIntSystemValueAndIntegrationSystemId(String mappingGroup, String mappingType, String intSystemValue, int integrationSystemId);

    MappingMetaData findByMappingGroupNameAndMappingTypeNameAndAvniValueAndIntegrationSystem(String mappingGroup, String mappingType, String avniValue, IntegrationSystem integrationSystem);
    MappingMetaData findByMappingGroupNameAndMappingTypeNameAndAvniValueAndIntegrationSystemId(String mappingGroup, String mappingType, String avniValue, int integrationSystemId);

    default MappingMetaData getAvniMappingIfPresent(String mappingGroup, String mappingType, String intSystemValue, IntegrationSystem integrationSystem) {
        return findByMappingGroupNameAndMappingTypeNameAndIntSystemValueAndIntegrationSystem(mappingGroup, mappingType, intSystemValue, integrationSystem);
    }

    //Repository should hide integrationSystemId
    @Deprecated
    default MappingMetaData getAvniMappingIfPresent(String mappingGroup, String mappingType, String intSystemValue, int integrationSystemId) {
        return findByMappingGroupNameAndMappingTypeNameAndIntSystemValueAndIntegrationSystemId(mappingGroup, mappingType, intSystemValue, integrationSystemId);
    }

    default MappingMetaData getAvniMapping(String mappingGroup, String mappingType, String intSystemValue) {
        return findByMappingGroupNameAndMappingTypeNameAndIntSystemValueAndIntegrationSystemId(mappingGroup, mappingType, intSystemValue, IntegrationContext.getIntegrationSystemId());
    }

    default MappingMetaData getIntSystemMappingIfPresent(String mappingGroup, String mappingType, String avniMapping, IntegrationSystem integrationSystem) {
        return findByMappingGroupNameAndMappingTypeNameAndAvniValueAndIntegrationSystem(mappingGroup, mappingType, avniMapping, integrationSystem);
    }

    default MappingMetaData getIntSystemMappingIfPresent(String mappingGroup, String mappingType, String avniMapping, int integrationSystemId) {
        return findByMappingGroupNameAndMappingTypeNameAndAvniValueAndIntegrationSystemId(mappingGroup, mappingType, avniMapping, integrationSystemId);
    }

    MappingMetaData findByMappingGroupAndMappingTypeAndIntSystemValueAndIsVoidedFalse(MappingGroup mappingGroup, MappingType mappingType, String intSystemValue);

    MappingMetaData findByMappingGroupAndMappingTypeAndAvniValueAndIsVoidedFalse(MappingGroup mappingGroup, MappingType mappingType, String avniValue);

    List<MappingMetaData> findAllByMappingGroupAndMappingType(MappingGroup mappingGroup, MappingType mappingType);
    List<MappingMetaData> findAllByMappingGroupAndMappingTypeAndIsVoidedFalse(MappingGroup mappingGroup, MappingType mappingType);

    List<MappingMetaData> findAllByMappingGroupAndMappingTypeInAndIsVoidedFalse(MappingGroup mappingGroup, List<MappingType> mappingTypes);

    List<MappingMetaData> findAllByMappingGroupNameAndIntegrationSystem(String mappingGroup, IntegrationSystem integrationSystem);

    List<MappingMetaData> findAllByMappingTypeAndIsVoidedFalse(MappingType mappingType);

    Page<MappingMetaData> findAllByAvniValueContainsAndIntegrationSystemAndIsVoidedFalse(String avniValue, IntegrationSystem integrationSystem, Pageable pageable);

    Page<MappingMetaData> findAllByIntSystemValueContainsAndIntegrationSystemAndIsVoidedFalse(String intSystemValue, IntegrationSystem integrationSystem, Pageable pageable);

    Page<MappingMetaData> findAllByAvniValueContainsAndIntSystemValueContainsAndIntegrationSystemAndIsVoidedFalse(String avniValue, String intSystemValue, IntegrationSystem integrationSystem, Pageable pageable);

    MappingMetaData findByMappingTypeAndIsVoidedFalse(MappingType mappingType);

    List<MappingMetaData> findAllByMappingTypeInAndAvniValueAndIsVoidedFalse(Collection<MappingType> mappingTypes, String avniValue);

    Page<MappingMetaData> findAllByIntegrationSystemAndIsVoidedFalse(IntegrationSystem currentIntegrationSystem, Pageable pageable);
    List<MappingMetaData> findAllByIntegrationSystem(IntegrationSystem currentIntegrationSystem);
}
