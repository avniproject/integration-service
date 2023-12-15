package org.avni_integration_service.lahi.config;

import org.avni_integration_service.common.PlatformException;
import org.avni_integration_service.common.UnknownException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LahiErrorTypeTest {
    @Test
    void getErrorType() {
        assertEquals(LahiErrorType.PlatformError, LahiErrorType.getErrorType(new PlatformException("")));
        assertEquals(LahiErrorType.CommonError, LahiErrorType.getErrorType(new UnknownException(new NullPointerException())));
    }
}
