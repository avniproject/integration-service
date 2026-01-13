package org.avni_integration_service.web.contract;

import org.avni_integration_service.integration_data.domain.MappingMetaData;

public class MappingMetadataContract extends MappingMetadataWebContract
{
    private String mappingGroupUuid;
    private String mappingTypeUuid;

    public String getMappingGroupUuid() {
        return mappingGroupUuid;
    }

    public void setMappingGroupUuid(String mappingGroupUuid) {
        this.mappingGroupUuid = mappingGroupUuid;
    }

    public String getMappingTypeUuid() {
        return mappingTypeUuid;
    }

    public void setMappingTypeUuid(String mappingTypeUuid) {
        this.mappingTypeUuid = mappingTypeUuid;
    }

    public static MappingMetadataContract fromMappingMetadata(MappingMetaData mappingMetaData) {
        MappingMetadataContract mappingMetadataContract = new MappingMetadataContract();
        mappingMetadataContract.setUuid(mappingMetaData.getUuid());
        mappingMetadataContract.setMappingGroupUuid(mappingMetaData.getMappingGroup().getUuid());
        mappingMetadataContract.setMappingTypeUuid(mappingMetaData.getMappingType().getUuid());
        mappingMetadataContract.setAvniValue(mappingMetaData.getAvniValue());
        mappingMetadataContract.setIntSystemValue(mappingMetaData.getIntSystemValue());
        mappingMetadataContract.setDataTypeHint(mappingMetaData.getDataTypeHint());
        return mappingMetadataContract;
    }
}
