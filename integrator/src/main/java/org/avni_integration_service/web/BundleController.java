package org.avni_integration_service.web;

import org.apache.log4j.Logger;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.repository.UserRepository;
import org.avni_integration_service.service.BundleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@PreAuthorize("hasRole('USER')")
public class BundleController extends BaseController {
    private final BundleService bundleService;
    private final Logger logger = Logger.getLogger(BundleController.class);


    @Autowired
    public BundleController(UserRepository userRepository, BundleService bundleService) {
        super(userRepository);
        this.bundleService = bundleService;
    }

    @RequestMapping(value = "/int/export", method = {RequestMethod.GET})
    public ResponseEntity<ByteArrayResource> exportMetadata(Principal principal) throws Exception {
        IntegrationSystem currentIntegrationSystem = getCurrentIntegrationSystem(principal);

        byte[] baosByteArray = bundleService.exportBundle(currentIntegrationSystem);

        return ResponseEntity.ok()
                .headers(getResponseHeaders(currentIntegrationSystem.getName()))
                .contentLength(baosByteArray.length)
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(new ByteArrayResource(baosByteArray));
    }

    @RequestMapping(value = "/int/import", method = {RequestMethod.POST})
    public ResponseEntity importMetadata(@RequestParam("bundleZip") MultipartFile bundleZip, Principal principal) {
        IntegrationSystem currentIntegrationSystem = getCurrentIntegrationSystem(principal);

        try {
            bundleService.importBundle(currentIntegrationSystem, bundleZip);
        } catch (Exception e) {
            logger.error(e);
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    private HttpHeaders getResponseHeaders(String integrationSystemName) {
        HttpHeaders headers = new HttpHeaders();
        String contentDisposition = "attachment; filename=" + integrationSystemName + ".zip";
        headers.add(HttpHeaders.CONTENT_DISPOSITION, contentDisposition);
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        headers.add(HttpHeaders.PRAGMA, "no-cache");
        headers.add(HttpHeaders.EXPIRES, "0");
        return headers;
    }
}


