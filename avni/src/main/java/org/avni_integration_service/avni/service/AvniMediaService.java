package org.avni_integration_service.avni.service;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;
import org.apache.log4j.Logger;
import org.avni_integration_service.avni.domain.AvniBaseContract;
import org.avni_integration_service.avni.domain.AvniMedia;
import org.avni_integration_service.avni.repository.AvniMediaRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public <T extends AvniMedia> List<T> getMediaForInsertion(AvniBaseContract avniBaseContract, String mediaField, List<T> sourceMediaList){
        List<String> avniUrls = (avniBaseContract != null) ?  (List<String>)  avniBaseContract.get(mediaField) : null;
        if(avniUrls==null || avniUrls.size()==0){
            return sourceMediaList;
        }
        return sourceMediaList.stream().filter(sourceMedia->!avniUrls.contains(sourceMedia.getAvniUrl())).collect(Collectors.toList());
    }


    public boolean uploadToAvni(String foldername, String fileName, MediaType mediatype) {
        String url = avniMediaRepository.generateUploadUrl(fileName);
        boolean result = avniMediaRepository.addMedia(url,foldername, fileName, mediatype);
        logger.info(String.format("File %s ||  Generated URL %s || isUploaded %s",fileName,url,result));
        return result;
    }
}
