package org.avni_integration_service.tanuh.service;

import org.avni_integration_service.avni.repository.AvniMediaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
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
        HttpClientErrorException forbidden = forbidden("User 'integration@tanuh_uat' not authorized to access '" + PHOTO + "'");
        when(media.getSignedDownloadUrl(PHOTO)).thenThrow(forbidden);

        PhotoOutsideStorageException e = assertThrows(PhotoOutsideStorageException.class, () -> downloader.sign(PHOTO));
        assertSame(forbidden, e.getCause());
    }

    // A 403 from anything else, such as the firewall in front of Avni blocking a burst of calls, says nothing about the
    // photo, so it is passed on and the screening waits for a retry rather than being marked not sent for good.
    @Test
    public void aForbiddenWithoutAvnisRefusalIsPassedOnAsItIs() {
        for (String body : new String[]{"<html><head><title>403 Forbidden</title></head><body><h1>403 Forbidden</h1></body></html>", ""}) {
            HttpClientErrorException forbidden = forbidden(body);
            doThrow(forbidden).when(media).getSignedDownloadUrl(PHOTO);

            assertSame(forbidden, assertThrows(HttpClientErrorException.class, () -> downloader.sign(PHOTO)), body);
        }
    }

    private static HttpClientErrorException forbidden(String body) {
        return HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden", HttpHeaders.EMPTY, body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    // Anything else is a failure to retry, never a verdict on the photo.
    @Test
    public void anyOtherFailureIsPassedOnAsItIs() {
        HttpServerErrorException down = HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", HttpHeaders.EMPTY, null, null);
        when(media.getSignedDownloadUrl(PHOTO)).thenThrow(down);

        assertSame(down, assertThrows(HttpServerErrorException.class, () -> downloader.sign(PHOTO)));
    }
}
