package org.avni_integration_service.goonj.service;


import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.avni_integration_service.avni.domain.AvniBaseContract;
import org.avni_integration_service.avni.service.AvniMediaService;
import org.avni_integration_service.goonj.domain.GoonjEntity;
import org.avni_integration_service.goonj.domain.GoonjMedia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GoonjMediaService {

    protected static final Logger logger = LoggerFactory.getLogger(BaseGoonjService.class);
    private final String AVNI_MEDIA_SOURCE = ""; //todo set with configuration

    public static final String LINK = "Link";
    public static final String IMAGE_ID = "ImageId";
    public static final String LOADING_AND_TRUCK_IMAGES = "Loading And Truck Images";
    public static final String IMAGES_LINK = "ImagesLink";
    public static final String IMAGE_EXTENSION = ".png"; //todo also handle for jpeg/jpg
    public static final String INVALID_PHOTOGRAPH_URLS_RECEIVED = "InvalidPhotographURLsReceived";


    private final RestTemplate restTemplate;
    private final AvniMediaService avniMediaService;

    public GoonjMediaService(@Qualifier("GoonjRestTemplate") RestTemplate restTemplate, AvniMediaService avniMediaService) {
        this.restTemplate = restTemplate;
        this.avniMediaService = avniMediaService;
    }

    private Map<GoonjMedia, Boolean> downloadMedia(List<GoonjMedia> goonjMediaList, String extension){
        logger.info("Number of media to be inserted : "+goonjMediaList.size());
        Map<GoonjMedia,Boolean> downloadResult = new HashMap<>();
        for (GoonjMedia goonjMedia : goonjMediaList) {
            Boolean isSuccessful = false;
            try {
                URI uri = new URI(goonjMedia.getExternalDownloadLink());
                isSuccessful = restTemplate.execute(uri, HttpMethod.GET, null, response -> {
                    logger.info(String.format("URL : %s || File : %s.%s || response code : %s || content-type : %s || content-length %s",
                            uri,
                            goonjMedia.getExternalId(),
                            extension,
                            response.getStatusCode(),
                            response.getHeaders().getContentType(),
                            response.getHeaders().getContentLength()));
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        //todo throw error
                        return false;
                    }
                    if (response.getHeaders().getContentType().toString().contains("text/html")) {
                        String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                        logger.info("Error HTML response: " + body);
                        return false;
                    }
                    File tempFile = new File(AvniMediaService.DIRECTORY_PATH, goonjMedia.getUuid() + extension);
                    FileOutputStream fileOutputStream = new FileOutputStream(tempFile, false);
                    IOUtils.copy(response.getBody(), fileOutputStream);
                    return true;
                });
            }  catch (Exception e) {
                logger.error("Error during download of media for "+goonjMedia.getExternalId(), e);
            }
            downloadResult.put(goonjMedia, isSuccessful);
        }
        return downloadResult;
    }

    private void uploadMedia(Map<GoonjMedia, Boolean> goonjMediaBooleanMap, String extension, MediaType mediatype) {
        for (Map.Entry<GoonjMedia, Boolean> entry : goonjMediaBooleanMap.entrySet()) {
            GoonjMedia goonjMedia = entry.getKey();
            Boolean wasDownloadedSuccessfully = entry.getValue();
            goonjMediaBooleanMap.put(goonjMedia, uploadMediaEntry(extension, mediatype, goonjMedia, wasDownloadedSuccessfully));
            deleteTempMediaFile(extension, goonjMedia);
        }
        logger.info("Download and Upload final result :"+ goonjMediaBooleanMap);
    }

    private boolean uploadMediaEntry(String extension, MediaType mediatype, GoonjMedia goonjMedia, Boolean wasDownloadedSuccessfully) {
        if (wasDownloadedSuccessfully) {
            return avniMediaService.uploadToAvni(AvniMediaService.DIRECTORY_PATH, goonjMedia.getUuid() + extension, mediatype);
        }
        return false;
    }

    private static void deleteTempMediaFile(String extension, GoonjMedia goonjMedia) {
        File file = new File(AvniMediaService.DIRECTORY_PATH, goonjMedia.getUuid() + extension);
        if(file.exists()){
            file.delete();
        }
    }

    public Map<GoonjMedia, Boolean> processMedia(AvniBaseContract storedEntity, List<GoonjMedia> goonjMediaList, MediaType mediatype, String avniImagesConceptName) {
        List<GoonjMedia> mediaForInsertion = avniMediaService.getMediaForInsertion(storedEntity, avniImagesConceptName, goonjMediaList);
        Map<GoonjMedia, Boolean> goonjMediaBooleanMap = downloadMedia(mediaForInsertion, IMAGE_EXTENSION);
        logger.info("Downloading result : " + goonjMediaBooleanMap);
        uploadMedia(goonjMediaBooleanMap, IMAGE_EXTENSION, mediatype);
        return goonjMediaBooleanMap;
    }

    public List<String> fetchListOfAvniUrlsToBeStoredAsConceptValue(List<GoonjMedia> goonjMediaList,
                                                                    String avniImagesConceptName, Map<GoonjMedia, Boolean> goonjMediaBooleanMap) {
        return goonjMediaList.stream()
                .filter(goonjMedia -> goonjMediaBooleanMap.get(goonjMedia) == null || goonjMediaBooleanMap.get(goonjMedia))
                .map(GoonjMedia::getAvniUrl)
                .toList();
    }

    public List<GoonjMedia> getSalesforceImageList(GoonjEntity goonjEntity, String goonjImagesFieldName){
        List<Map<String, String>> mediaMap = (List<Map<String, String>>) goonjEntity.getValue(goonjImagesFieldName);
        if(mediaMap != null) {
            return mediaMap.stream().map(mediaResponse -> {
                String externalId= mediaResponse.get(IMAGE_ID);
                String uuid = avniMediaService.getUniqueUUIDFORMediaUniqueID(externalId);
                return new GoonjMedia(mediaResponse.get(LINK).trim(), externalId,
                        AVNI_MEDIA_SOURCE + uuid + IMAGE_EXTENSION, uuid);
            }).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public Boolean hasAtleastOneInvalidImagesLink(Map<GoonjMedia, Boolean> goonjMediaBooleanMap) {
        return goonjMediaBooleanMap.values().stream().anyMatch(value -> !value);
    }
}
