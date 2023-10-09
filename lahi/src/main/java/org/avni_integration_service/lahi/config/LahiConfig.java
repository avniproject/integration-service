package org.avni_integration_service.lahi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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



    }
