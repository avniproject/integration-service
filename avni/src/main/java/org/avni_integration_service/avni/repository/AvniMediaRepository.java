package org.avni_integration_service.avni.repository;

import org.avni_integration_service.avni.client.AvniHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class AvniMediaRepository {

    private final AvniHttpClient avniHttpClient;

    public AvniMediaRepository(AvniHttpClient avniHttpClient) {
        this.avniHttpClient = avniHttpClient;
    }

    public String generateUploadUrl(String fileName){
        ResponseEntity<String> responseEntity = avniHttpClient.get(String.format("/media/uploadUrl/%s", fileName), String.class);
        return responseEntity.getBody();
    }

    public boolean addMedia(String generatedUrl, String foldername, String filename, MediaType mediatype){
        ResponseEntity<String> responseEntity = avniHttpClient.putMedia(generatedUrl, foldername,filename, mediatype);
        return responseEntity.getStatusCode().is2xxSuccessful();
    }
}
