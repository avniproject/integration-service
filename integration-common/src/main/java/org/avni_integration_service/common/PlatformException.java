package org.avni_integration_service.common;

/**
 * Use this exception to indicate that integration service module cannot process any messages due to problems in configuration of integration service, integration system, or avni.
 */
public class PlatformException extends Exception {
    public PlatformException(String message) {
        super(message);
    }

    public PlatformException(Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
