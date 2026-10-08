package org.avni_integration_service.bahmni.repository.openmrs;

import org.avni_integration_service.bahmni.client.OpenMRSWebClient;
import org.avni_integration_service.bahmni.contract.OpenMRSSavePerson;
import org.avni_integration_service.bahmni.contract.OpenMRSUuidHolder;
import org.avni_integration_service.bahmni.repository.BaseOpenMRSRepository;
import org.avni_integration_service.util.ObjectJsonMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpenMRSPersonRepository extends BaseOpenMRSRepository {
    @Autowired
    public OpenMRSPersonRepository(OpenMRSWebClient openMRSWebClient) {
        super(openMRSWebClient);
    }

    public OpenMRSUuidHolder createPerson(OpenMRSSavePerson openMRSSavePerson) {
        String json = ObjectJsonMapper.writeValueAsString(openMRSSavePerson);
        String outputJson = openMRSWebClient.post(getResourcePath("person"), json);
        return ObjectJsonMapper.readValue(outputJson, OpenMRSUuidHolder.class);
    }

    public void setPersonAttribute(String personUuid, String attributeTypeUuid, String value, String existingAttributeUuid) {
        String payload = String.format("{\"attributeType\": \"%s\", \"value\": \"%s\"}", attributeTypeUuid, value);
        String path = String.format("person/%s/attribute", personUuid);
        if (existingAttributeUuid != null) {
            openMRSWebClient.post(getSingleResourcePath(path, existingAttributeUuid), payload);
        } else {
            openMRSWebClient.post(getResourcePath(path), payload);
        }
    }
}
