package org.avni_integration_service.common;

/**
 * Use this to indicate the source of problem in processing is unknown
 */
public class UnknownException extends Exception {
    public UnknownException(Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
