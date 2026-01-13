package org.avni_integration_service.util;

import org.springframework.util.ObjectUtils;

public class ObjectUtil {
    public static boolean nullSafeEqualsIgnoreCase(Object o1, Object o2) {
        if (ObjectUtils.nullSafeEquals(o1, o2)) {
            return true;
        }
        if (o1 != null && o2 != null) {
            return ObjectUtil.nullSafeEqualsIgnoreCase((String) o1, (String) o2);
        }
        return false;
    }

    public static boolean nullSafeEqualsIgnoreCase(String o1, String o2) {
        if (ObjectUtils.nullSafeEquals(o1, o2)) {
            return true;
        }
        if (o1 != null && o2 != null) {
            return o1.equalsIgnoreCase(o2);
        }
        return false;
    }
}
