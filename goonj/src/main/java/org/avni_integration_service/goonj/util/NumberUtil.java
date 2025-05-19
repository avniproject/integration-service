package org.avni_integration_service.goonj.util;

public class NumberUtil {
    public static Integer getInteger(Object value) {
        if (value == null) {
            return null;
        }
        return Double.valueOf(value.toString()).intValue();
    }

    public static Double getDouble(Object value) {
        if (value == null) {
            return null;
        }
        return Double.valueOf(value.toString());
    }
}
