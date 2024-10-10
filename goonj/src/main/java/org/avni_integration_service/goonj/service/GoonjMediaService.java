package org.avni_integration_service.goonj.service;


import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.avni_integration_service.avni.domain.AvniBaseContract;
import org.avni_integration_service.avni.service.AvniMediaService;
import org.avni_integration_service.goonj.config.GoonjContextProvider;
import org.avni_integration_service.goonj.domain.GoonjEntity;
import org.avni_integration_service.goonj.domain.GoonjMedia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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
    public static final String LINK = "Link";
    public static final String IMAGE_ID = "ImageId";
    public static final String LOADING_AND_TRUCK_IMAGES = "Loading And Truck Images";
    public static final String IMAGES_LINK = "ImagesLink";
    public static final String INVALID_PHOTOGRAPH_URLS_RECEIVED = "InvalidPhotographURLsReceived";


    private final RestTemplate restTemplate;
    private final AvniMediaService avniMediaService;
    private final GoonjContextProvider goonjContextProvider;

    public GoonjMediaService(@Qualifier("GoonjRestTemplate") RestTemplate restTemplate, AvniMediaService avniMediaService,GoonjContextProvider goonjContextProvider) {
        this.restTemplate = restTemplate;
        this.avniMediaService = avniMediaService;
        this.goonjContextProvider = goonjContextProvider;
    }

    private Map<GoonjMedia, Boolean> downloadMedia(List<GoonjMedia> goonjMediaList, String allowedMedia){
        logger.info("Number of media to be inserted : "+goonjMediaList.size());
        Map<GoonjMedia,Boolean> downloadResult = new HashMap<>();
        for (GoonjMedia goonjMedia : goonjMediaList) {
            Boolean isSuccessful = false;
            try {
                URI uri = new URI(goonjMedia.getExternalDownloadLink());
                isSuccessful = restTemplate.execute(uri, HttpMethod.GET, null, response -> {
                    MediaType contentType = response.getHeaders().getContentType();
                    HttpStatus statusCode = response.getStatusCode();
                    logger.info(String.format("URL : %s || File : %s || response code : %s || content-type : %s || content-length %s",
                            uri,
                            goonjMedia.getExternalId(),
                            statusCode,
                            contentType,
                            response.getHeaders().getContentLength()));
                    MediaType avniContentType = avniMediaService.checkAndGetMediaType(allowedMedia, contentType);
                    if(statusCode.is2xxSuccessful() && avniContentType!=null){
                        String avniMediaPath = goonjContextProvider.get().getS3Url();
                        String extension = contentType.getSubtype();
                        String uuid = goonjMedia.getUuid();
                        goonjMedia.setExtention(extension);
                        goonjMedia.setAvniUrl(String.format("%s%s.%s",avniMediaPath,uuid,extension));
                        goonjMedia.setContentType(avniContentType);
                        File tempFile = new File(AvniMediaService.DIRECTORY_PATH, String.format("%s.%s",uuid,extension));
                        FileOutputStream fileOutputStream = new FileOutputStream(tempFile, false);
                        IOUtils.copy(response.getBody(), fileOutputStream);
                        return true;
                    }
                    else {
                        if (contentType.equalsTypeAndSubtype(MediaType.TEXT_HTML)) {
                            String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                            logger.error("Errored HTML response: " + body);
                            return false;
                        }
                    }
                    return false;
                });
            }  catch (Exception e) {
                logger.error("Error during download of media for "+goonjMedia.getExternalId(), e);
            }
            downloadResult.put(goonjMedia, isSuccessful);
        }
        logger.info("Downloading result : " + downloadResult);
        return downloadResult;
    }

    private void uploadMedia(Map<GoonjMedia, Boolean> goonjMediaBooleanMap) {
        for (Map.Entry<GoonjMedia, Boolean> entry : goonjMediaBooleanMap.entrySet()) {
            GoonjMedia goonjMedia = entry.getKey();
            Boolean wasDownloadedSuccessfully = entry.getValue();
            goonjMediaBooleanMap.put(goonjMedia, uploadMediaEntry(goonjMedia, wasDownloadedSuccessfully));
            if(wasDownloadedSuccessfully) {
                deleteTempMediaFile(goonjMedia);
            }
        }
        logger.info("Download and Upload final result :"+ goonjMediaBooleanMap);
    }

    private boolean uploadMediaEntry(GoonjMedia goonjMedia, Boolean wasDownloadedSuccessfully) {
        if (wasDownloadedSuccessfully) {
            String fileName = String.format("%s.%s",goonjMedia.getUuid(),goonjMedia.getExtention());
            return avniMediaService.uploadToAvni(AvniMediaService.DIRECTORY_PATH, fileName, goonjMedia.getContentType());
        }
        return false;
    }

    private static void deleteTempMediaFile(GoonjMedia goonjMedia) {
        String fileName = String.format("%s.%s",goonjMedia.getUuid(),goonjMedia.getExtention());
        File file = new File(AvniMediaService.DIRECTORY_PATH, fileName);
        if(file.exists()){
            file.delete();
        }
    }

    public Map<GoonjMedia, Boolean> processMedia(List<String> storedAvniUrls, List<GoonjMedia> goonjMediaList,String avniMediaType) {
        List<GoonjMedia> mediaForInsertion = avniMediaService.processSourceMediaListAndGetMediaForInsertion(storedAvniUrls, goonjMediaList);
        Map<GoonjMedia, Boolean> goonjMediaBooleanMap = downloadMedia(mediaForInsertion, avniMediaType);
        uploadMedia(goonjMediaBooleanMap);
        return goonjMediaBooleanMap;
    }

    public List<String> fetchListOfAvniUrlsToBeStoredAsConceptValue(List<GoonjMedia> goonjMediaList,Map<GoonjMedia, Boolean> goonjMediaDownloadAndUploadResultMap) {
        return goonjMediaList.stream()
                .filter(goonjMedia -> {
                    if(goonjMediaDownloadAndUploadResultMap.containsKey(goonjMedia)){
                        return goonjMediaDownloadAndUploadResultMap.get(goonjMedia);
                    }
                    else{
                        return goonjMedia.getAvniUrl()!=null;
                    }
                })
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
                        null, uuid,null,null);
            }).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public Boolean hasAtleastOneInvalidImagesLink(Map<GoonjMedia, Boolean> goonjMediaBooleanMap) {
        return goonjMediaBooleanMap.values().stream().anyMatch(value -> !value);
    }

    public List<String> getStoredMediaUrls(AvniBaseContract avniBaseContract,String avniMediaField){
        return avniMediaService.getCodedStoredConceptValue(avniBaseContract,avniMediaField);
    }
}
