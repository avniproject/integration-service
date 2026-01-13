package org.avni_integration_service.common;

/**
 * Use this exception to indicate that the current message cannot be processed due to problems with this message
 */
public class MessageUnprocessableException extends Exception {
    public MessageUnprocessableException(String message) {
        super(message);
    }
}
