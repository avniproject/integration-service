package org.avni_integration_service.tanuh.service;

import org.avni_integration_service.avni.repository.AvniMediaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TanuhPhotoDownloaderTest {
    private static final String PHOTO = "https://s3.ap-south-1.amazonaws.com/prod-user-media/tanuh_uat/s1-0.jpg";
    private final AvniMediaRepository media = mock(AvniMediaRepository.class);
    private final TanuhPhotoDownloader downloader = new TanuhPhotoDownloader(media);

    @Test
    public void aSignedLinkIsReturned() {
        when(media.getSignedDownloadUrl(PHOTO)).thenReturn(PHOTO + "?X-Amz-Signature=x");
        assertEquals(PHOTO + "?X-Amz-Signature=x", downloader.sign(PHOTO));
    }

    // avni-server answers 403 for a photo in another organisation's folder or another bucket (MediaController,
    // AWSS3Service.generateMediaDownloadUrl), so the photo is outside the organisation's media storage.
    @Test
    public void avnisRefusalMeansThePhotoIsOutsideTheOrganisationsStorage() {
        HttpClientErrorException forbidden = HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden", HttpHeaders.EMPTY, null, null);
        when(media.getSignedDownloadUrl(PHOTO)).thenThrow(forbidden);

        PhotoOutsideStorageException e = assertThrows(PhotoOutsideStorageException.class, () -> downloader.sign(PHOTO));
        assertSame(forbidden, e.getCause());
    }

    // Anything else is a failure to retry, never a verdict on the photo.
    @Test
    public void anyOtherFailureIsPassedOnAsItIs() {
        HttpServerErrorException down = HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", HttpHeaders.EMPTY, null, null);
        when(media.getSignedDownloadUrl(PHOTO)).thenThrow(down);

        assertSame(down, assertThrows(HttpServerErrorException.class, () -> downloader.sign(PHOTO)));
    }
}
