package org.avni_integration_service.goonj.domain;

import org.avni_integration_service.util.MapUtil;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

public interface GoonjEntity {

    public static String AvniStateField = "State";
    public static String AvniDistrictField = "District";

    List<String> getObservationFields();

    Object getValue(String responseField);

    default String getAddress(String stateField, String districtField, Map<String, Object> response) {
        String state = MapUtil.getString(stateField, response);
        String district = MapUtil.getString(districtField, response);
        if(StringUtils.hasText(state) && StringUtils.hasText(district)) {
            return state + ", " + district;
        }
        throw new RuntimeException("Invalid address value specified");
    }
    default Map<String,String> getAddressMap(String stateField, String districtField, Map<String, Object> response){
        String state = MapUtil.getString(stateField, response);
        String district = MapUtil.getString(districtField, response);
        if(StringUtils.hasText(state) && StringUtils.hasText(district)) {
            return Map.of(AvniStateField,state,AvniDistrictField,district);
        }
        throw new RuntimeException("Invalid address value specified");
    }
}
