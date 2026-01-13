package org.avni_integration_service.goonj.util;

import org.avni_integration_service.util.ObjectJsonMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.avni_integration_service.goonj.util.NumberUtil.getDouble;
import static org.avni_integration_service.goonj.util.NumberUtil.getInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NumberUtilTest {

    @Test
    public void shouldConvertToInteger() {
        Map<String, Object> map = ObjectJsonMapper.readValue(
                "{              \n" +
                        "  \"a\": \"1\",    \n" +
                        "  \"b\": 1,        \n" +
                        "  \"c\": 1.5       \n" +
                        "}", java.util.Map.class);
        assertEquals(getInteger(map.get("a")), 1);
        assertEquals(getInteger(map.get("b")), 1);
        assertEquals(getInteger(map.get("c")), 1);
        assertEquals(getInteger(map.get("d")), null);
    }

    @Test
    public void shouldConvertToDouble() {
        Map<String, Object> map = ObjectJsonMapper.readValue(
                "{              \n" +
                        "  \"a\": \"1\",    \n" +
                        "  \"b\": 1,        \n" +
                        "  \"c\": 1.5,      \n" +
                        "  \"e\": \"1.2345\" \n" +
                        "}", java.util.Map.class);

        assertEquals(getDouble(map.get("a")), Double.valueOf(1.0));
        assertEquals(getDouble(map.get("b")), Double.valueOf(1.0));
        assertEquals(getDouble(map.get("c")), Double.valueOf(1.5));
        assertEquals(getDouble(map.get("e")), Double.valueOf(1.2345));
    }

    @Test
    public void shouldThrowClassCastExceptionWhenCastingIntegerToDouble() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", Integer.valueOf(1));
        map.put("value1", String.valueOf(1));
        assertThrows(ClassCastException.class, () -> {
            Double d = (Double) map.get("value");
        });
        assertThrows(ClassCastException.class, () -> {
            Double d = (Double) map.get("value1");
        });
    }
}
