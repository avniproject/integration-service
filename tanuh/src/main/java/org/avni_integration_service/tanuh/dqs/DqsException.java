package org.avni_integration_service.tanuh.dqs;

// The data quality service did not take a record: it could not be reached, refused the caller or the request, or has
// no route for the record. The screening waits in an error record and the error job sends it again.
public class DqsException extends RuntimeException {
    public DqsException(String message) {
        super(message);
    }

    public DqsException(String message, Throwable cause) {
        super(message, cause);
    }
}
