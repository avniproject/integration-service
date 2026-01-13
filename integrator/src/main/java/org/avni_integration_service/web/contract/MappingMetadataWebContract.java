package org.avni_integration_service.web.contract;

import org.avni_integration_service.util.ObsDataType;

public class MappingMetadataWebContract extends BaseIntSystemSpecificContract {
    private int mappingGroup;
    private int mappingType;
    private String avniValue;
    private String intSystemValue;
    private ObsDataType dataTypeHint;
    private String about;

    public int getMappingGroup() {
        return mappingGroup;
    }

    public void setMappingGroup(int mappingGroup) {
        this.mappingGroup = mappingGroup;
    }

    public int getMappingType() {
        return mappingType;
    }

    public void setMappingType(int mappingType) {
        this.mappingType = mappingType;
    }

    public String getAvniValue() {
        return avniValue;
    }

    public void setAvniValue(String avniValue) {
        this.avniValue = avniValue;
    }

    public String getIntSystemValue() {
        return intSystemValue;
    }

    public void setIntSystemValue(String intSystemValue) {
        this.intSystemValue = intSystemValue;
    }

    public ObsDataType getDataTypeHint() {
        return dataTypeHint;
    }

    public void setDataTypeHint(ObsDataType dataTypeHint) {
        this.dataTypeHint = dataTypeHint;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }
}
