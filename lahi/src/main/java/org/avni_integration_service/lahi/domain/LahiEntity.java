package org.avni_integration_service.lahi.domain;

import java.util.List;

public interface LahiEntity {
    List<String> getObservationFields();
    Object getValue(String responseField);
}
