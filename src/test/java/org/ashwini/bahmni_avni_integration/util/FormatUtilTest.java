package org.ashwini.bahmni_avni_integration.util;

import org.junit.jupiter.api.Test;

import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.*;

public class FormatUtilTest {
    @Test
    public void toISODateString() {
        GregorianCalendar gregorianCalendar = new GregorianCalendar(2020, 2, 3);
        assertEquals("2020-03-03T00:00:00.000Z", FormatUtil.toISODateString(gregorianCalendar.getTime()));
    }
}