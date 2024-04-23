package org.avni_integration_service.goonj.util;

import org.avni_integration_service.util.ObjectJsonMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.avni_integration_service.goonj.util.NumberUtil.getInteger;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

}
