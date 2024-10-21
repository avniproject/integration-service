package org.avni_integration_service.goonj.exceptions;

import org.springframework.http.HttpStatus;

public class GoonjAdhocException extends RuntimeException{
    private HttpStatus httpStatus;

    public GoonjAdhocException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

}
