package org.avni_integration_service.avni.service;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.AvniBaseContract;
import org.avni_integration_service.avni.domain.AvniMedia;
import org.avni_integration_service.avni.domain.AvniMediaConstants;
import org.avni_integration_service.avni.repository.AvniMediaRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Service
public class AvniMediaService {
    private static final Logger logger = Logger.getLogger(AvniMediaService.class);
    private final UUID namespace = NameBasedGenerator.NAMESPACE_URL;
    public static final  String DIRECTORY_PATH = "/tmp/";
    private final AvniMediaRepository avniMediaRepository;

    public AvniMediaService(AvniMediaRepository avniMediaRepository) {
        this.avniMediaRepository = avniMediaRepository;
    }

    public String getUniqueUUIDFORMediaUniqueID(String uniqueId){
        return Generators.nameBasedGenerator().generate(uniqueId).toString();
    }

    public <T extends AvniMedia> List<T> processSourceMediaListAndGetMediaForInsertion(List<String> storedAvniUrls, List<T> sourceMediaList){
        List<T> insertionMedia = new LinkedList<>();
        if(storedAvniUrls == null ||storedAvniUrls.size()==0){
            return sourceMediaList;
        }
        sourceMediaList.forEach(sourceMedia->{
            String avniUrlFromExternalId = getAvniUrlFromExternalId(storedAvniUrls, sourceMedia.getExternalId());
            if(avniUrlFromExternalId!=null){
                sourceMedia.setAvniUrl(avniUrlFromExternalId);
            }
            else{
                insertionMedia.add(sourceMedia);
            }
        });
        return insertionMedia;
    }


    public boolean uploadToAvni(String foldername, String fileName, MediaType mediatype) {
        String url = avniMediaRepository.generateUploadUrl(fileName);
        boolean result = avniMediaRepository.addMedia(url,foldername, fileName, mediatype);
        logger.info(String.format("File %s ||  Generated URL %s || isUploaded %s",fileName,url,result));
        return result;
    }

    public MediaType checkAndGetMediaType(String avniMediaType, MediaType contentType){
        switch (avniMediaType){
            case AvniMediaConstants.IMAGE:
                return AvniMediaConstants.SUPPORTED_IMAGE_MEDIATYPE_SET.stream().filter(contentType::equalsTypeAndSubtype).findFirst().orElse(null);
            default:
                return null;
        }
    }

    public List<String> getCodedStoredConceptValue(AvniBaseContract avniBaseContract,String avniMediaField){
        List<String> avniUrls = (avniBaseContract == null) ? null : (List<String>) avniBaseContract.getObservation(avniMediaField);
        if(avniUrls == null ){
            return Collections.emptyList();
        }
        return avniUrls;
    }

    public String getAvniUrlFromExternalId(List<String> avniUrls, String externalId){
        return avniUrls.stream().filter(avniUrl->avniUrl.contains(getUniqueUUIDFORMediaUniqueID(externalId))).findFirst().orElse(null);
    }

}
