package org.avni_integration_service.lahi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class LahiConfig {
    @Value("${lahi.glific.apiKey}")
    private String glificAPIKey;

    @Value("${lahi.glific.apiToken}")
    private String glificAPIToken;

    @Value("${lahi.glific.accountSID}")
    private String glificAccountSID;

    @Value("${lahi.glific.subdomain}")
    private String glificSubdomain;

    public String getLahiAPIKey() {
        return glificAPIKey;
    }

    public String getLahiAPIToken() {
        return glificAPIToken;
    }

    public String getLahiAccountSID() {
        return glificAccountSID;
    }

    public String getLahiSubdomain() {
        return glificSubdomain;
    }

    public String getCallDetailsAPI(String sid) {
        String baseURI = String.format("https://%s/v1/Accounts/%s/Calls", this.glificSubdomain, this.glificAccountSID);
        String uri = (sid == null ? baseURI : baseURI + String.format("/%s", sid));
        return uri + ".json"; //To inform Lahi to always send response in JSON format
    }

    @Bean("LahiRestTemplate")
    RestTemplate restTemplate() {
        return new RestTemplateBuilder()
                .build();
        //TODO
    }
}
