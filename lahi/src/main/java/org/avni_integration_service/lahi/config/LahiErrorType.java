package org.avni_integration_service.lahi.config;

import org.avni_integration_service.common.PlatformException;
import org.avni_integration_service.common.UnknownException;

public enum LahiErrorType {
    CommonError,
    PlatformError;

    public static LahiErrorType getErrorType(Exception e) {
        if (e instanceof PlatformException) return PlatformError;
        if (e instanceof UnknownException) return CommonError;
        return null;
    }
}
