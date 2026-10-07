package org.avni_integration_service.tanuh.service;

import org.avni_integration_service.avni.repository.AvniMediaRepository;
import org.avni_integration_service.avni.service.AvniMediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.time.Duration;

@Component
public class TanuhPhotoDownloader {
    static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final AvniMediaRepository avniMediaRepository;
    private final RestTemplate restTemplate;
    private final File directory;

    @Autowired
    public TanuhPhotoDownloader(AvniMediaRepository avniMediaRepository) {
        this(avniMediaRepository, restTemplateWithTimeout(TIMEOUT), new File(AvniMediaService.DIRECTORY_PATH));
    }

    TanuhPhotoDownloader(AvniMediaRepository avniMediaRepository, RestTemplate restTemplate, File directory) {
        this.avniMediaRepository = avniMediaRepository;
        this.restTemplate = restTemplate;
        this.directory = directory;
    }

    // The module's own template: GoonjRestTemplate adds a Salesforce token to every request,
    // and the shared Avni template has no working timeout.
    static RestTemplate restTemplateWithTimeout(Duration timeout) {
        return new RestTemplateBuilder().setConnectTimeout(timeout).setReadTimeout(timeout).build();
    }

    // Signs just before the download, since a link lasts two minutes. The caller deletes the file.
    public File download(String s3Url) {
        URI signedUrl = URI.create(avniMediaRepository.getSignedDownloadUrl(s3Url));
        return restTemplate.execute(signedUrl, HttpMethod.GET, null, response -> {
            // Owner-only (0600): these are patients' mouth photos.
            File file = Files.createTempFile(directory.toPath(), "tanuh-photo-", null).toFile();
            try (InputStream body = response.getBody(); OutputStream out = new FileOutputStream(file)) {
                body.transferTo(out);
                return file;
            } catch (IOException | RuntimeException e) {
                file.delete();
                throw e;
            }
        });
    }
}
