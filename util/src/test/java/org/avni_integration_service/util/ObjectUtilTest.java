package org.avni_integration_service.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilTest {
    @Test
    public void nullSafeEqualsIgnoreCase() {
        assertTrue(ObjectUtil.nullSafeEqualsIgnoreCase("A", "a"));
        assertTrue(ObjectUtil.nullSafeEqualsIgnoreCase("A", "A"));
        assertTrue(ObjectUtil.nullSafeEqualsIgnoreCase(null, null));
        assertFalse(ObjectUtil.nullSafeEqualsIgnoreCase(null, "a"));
        assertFalse(ObjectUtil.nullSafeEqualsIgnoreCase("a", "b"));
    }
}
