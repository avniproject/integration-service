package org.avni_integration_service.goonj.exceptions;


import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class GoonjAvniRestException extends RuntimeException {
    private HttpHeaders httpHeaders;
    private HttpStatus httpStatus;
    private String message;
    private Map<String,Object> errorBody;

    public GoonjAvniRestException(HttpHeaders httpHeaders, HttpStatus httpStatus, String message, Map<String,Object> errorBody) {
        super(message);
        this.httpHeaders = httpHeaders;
        this.httpStatus = httpStatus;
        this.message = message;
        this.errorBody = errorBody;
    }

    public HttpHeaders getHttpHeaders() {
        return httpHeaders;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public Map<String,Object> getErrorBody() {
        return errorBody;
    }
}
