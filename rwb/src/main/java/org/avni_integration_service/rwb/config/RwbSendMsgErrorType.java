package org.avni_integration_service.rwb.config;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

public enum RwbSendMsgErrorType {
    BadRequest,
    BadConfiguration,
    RuntimeError;

    public static RwbSendMsgErrorType getErrorType(Exception e) {
        if (e instanceof HttpClientErrorException) return BadRequest;
        if (e instanceof HttpServerErrorException.InternalServerError) return RuntimeError;
        if (e instanceof HttpServerErrorException) return BadConfiguration;
        return null;
    }
}
