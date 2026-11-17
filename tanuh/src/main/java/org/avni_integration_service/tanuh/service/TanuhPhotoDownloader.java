package org.avni_integration_service.tanuh.service;

import org.avni_integration_service.avni.repository.AvniMediaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

// Signs a photo's stored address so the data quality service can read it. Tanuh's model service reads the photos
// from S3 itself, so nothing is downloaded here any more.
@Component
public class TanuhPhotoDownloader {
    private final AvniMediaRepository avniMediaRepository;

    @Autowired
    public TanuhPhotoDownloader(AvniMediaRepository avniMediaRepository) {
        this.avniMediaRepository = avniMediaRepository;
    }

    // avni-server's refusal for a photo in another organisation's folder or another bucket (AWSS3Service, 18.2 and 18.3).
    // It is in the 403's body while the server sends exception messages, as production does (avni-infra prod_vars).
    static final String AVNI_REFUSAL = "not authorized to access";

    // A link Avni signs lasts two minutes, so it is asked for just before it is used. Only Avni's own refusal says the
    // photo is outside the organisation's media storage. Any other failure, a 403 from the firewall in front of Avni
    // included, says nothing about the photo, so it is passed on as it is and the screening waits for a retry.
    public String sign(String s3Url) {
        try {
            return avniMediaRepository.getSignedDownloadUrl(s3Url);
        } catch (HttpClientErrorException.Forbidden e) {
            if (!e.getResponseBodyAsString().contains(AVNI_REFUSAL)) throw e;
            throw new PhotoOutsideStorageException("Avni will not sign the photo: it is outside the organisation's media storage", e);
        }
    }
}
