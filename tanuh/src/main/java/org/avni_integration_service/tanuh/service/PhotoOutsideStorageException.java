package org.avni_integration_service.tanuh.service;

// Avni answered 403 when asked to sign a photo: the photo is outside the organisation's own media storage.
public class PhotoOutsideStorageException extends RuntimeException {
    public PhotoOutsideStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
