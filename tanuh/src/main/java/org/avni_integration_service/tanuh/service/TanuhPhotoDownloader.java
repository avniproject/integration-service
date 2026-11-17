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

    // A link Avni signs lasts two minutes, so it is asked for just before it is used. Avni answers 403 for a photo in
    // another organisation's folder or another bucket; any other failure is passed on as it is, to be retried.
    public String sign(String s3Url) {
        try {
            return avniMediaRepository.getSignedDownloadUrl(s3Url);
        } catch (HttpClientErrorException.Forbidden e) {
            throw new PhotoOutsideStorageException("Avni will not sign the photo: it is outside the organisation's media storage", e);
        }
    }
}
