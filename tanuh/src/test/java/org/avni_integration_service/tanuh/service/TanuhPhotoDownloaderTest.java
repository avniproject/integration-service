package org.avni_integration_service.tanuh.service;

import org.avni_integration_service.avni.repository.AvniMediaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TanuhPhotoDownloaderTest {
    private static final String STORED = "https://s3.ap-south-1.amazonaws.com/prod-user-media/tanuh/photo.jpg";
    private static final String SIGNED = "https://prod-user-media.s3.ap-south-1.amazonaws.com/tanuh/photo.jpg?X-Amz-Credential=AKIA%2F20261007%2Fap-south-1&X-Amz-Signature=abc";

    @Mock
    private AvniMediaRepository avniMediaRepository;
    @Mock
    private RestTemplate restTemplate;
    @TempDir
    File directory;

    static class TrackingStream extends ByteArrayInputStream {
        boolean closed;

        TrackingStream(byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    static class BreakingStream extends InputStream {
        boolean closed;
        int served;

        @Override
        public int read() throws IOException {
            if (served++ < 2) return 7;
            throw new IOException("connection reset");
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private void respondWith(InputStream body) {
        when(restTemplate.execute(any(URI.class), eq(HttpMethod.GET), isNull(), any(ResponseExtractor.class)))
                .thenAnswer(invocation -> ((ResponseExtractor<?>) invocation.getArgument(3))
                        .extractData(new MockClientHttpResponse(body, HttpStatus.OK)));
    }

    @Test
    public void writesThePhotoFromTheSignedLinkAndClosesTheStream() throws Exception {
        when(avniMediaRepository.getSignedDownloadUrl(STORED)).thenReturn(SIGNED);
        byte[] photo = {1, 2, 3, 4};
        TrackingStream body = new TrackingStream(photo);
        respondWith(body);

        File file = new TanuhPhotoDownloader(avniMediaRepository, restTemplate, directory).download(STORED);

        assertArrayEquals(photo, Files.readAllBytes(file.toPath()));
        assertEquals(directory, file.getParentFile());
        assertTrue(body.closed);
        ArgumentCaptor<URI> uri = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).execute(uri.capture(), eq(HttpMethod.GET), isNull(), any(ResponseExtractor.class));
        assertEquals(SIGNED, uri.getValue().toString(), "the signed link must be sent as it is, %2F not re-encoded");
    }

    @Test
    public void theDownloadedPhotoIsReadableOnlyByTheServiceUser() throws Exception {
        when(avniMediaRepository.getSignedDownloadUrl(STORED)).thenReturn(SIGNED);
        respondWith(new TrackingStream(new byte[]{1, 2, 3}));

        File file = new TanuhPhotoDownloader(avniMediaRepository, restTemplate, directory).download(STORED);

        assertEquals(EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
                Files.getPosixFilePermissions(file.toPath()));
    }

    @Test
    public void aFailedDownloadSurfacesToTheCaller() {
        when(avniMediaRepository.getSignedDownloadUrl(STORED)).thenReturn(SIGNED);
        ResourceAccessException timeout = new ResourceAccessException("Read timed out");
        when(restTemplate.execute(any(URI.class), eq(HttpMethod.GET), isNull(), any(ResponseExtractor.class))).thenThrow(timeout);

        TanuhPhotoDownloader downloader = new TanuhPhotoDownloader(avniMediaRepository, restTemplate, directory);

        assertSame(timeout, assertThrows(ResourceAccessException.class, () -> downloader.download(STORED)));
    }

    @Test
    public void aDownloadThatBreaksOffLeavesNoFileAndClosesTheStream() {
        when(avniMediaRepository.getSignedDownloadUrl(STORED)).thenReturn(SIGNED);
        BreakingStream body = new BreakingStream();
        respondWith(body);

        TanuhPhotoDownloader downloader = new TanuhPhotoDownloader(avniMediaRepository, restTemplate, directory);

        assertThrows(IOException.class, () -> downloader.download(STORED));
        assertEquals(0, directory.listFiles().length);
        assertTrue(body.closed);
    }

    @Test
    public void aStalledDownloadGivesUpAfterTheReadTimeout() throws Exception {
        try (ServerSocket server = new ServerSocket(0)) {
            Thread acceptor = new Thread(() -> {
                try (Socket ignored = server.accept()) {
                    Thread.sleep(10_000);
                } catch (Exception ignored) {
                }
            });
            acceptor.setDaemon(true);
            acceptor.start();
            when(avniMediaRepository.getSignedDownloadUrl(STORED)).thenReturn("http://localhost:" + server.getLocalPort() + "/tanuh/photo.jpg");
            TanuhPhotoDownloader downloader = new TanuhPhotoDownloader(avniMediaRepository,
                    TanuhPhotoDownloader.restTemplateWithTimeout(Duration.ofMillis(500)), directory);

            assertTimeoutPreemptively(Duration.ofSeconds(5),
                    () -> assertThrows(ResourceAccessException.class, () -> downloader.download(STORED)));
            assertEquals(0, directory.listFiles().length);
        }
    }
}
