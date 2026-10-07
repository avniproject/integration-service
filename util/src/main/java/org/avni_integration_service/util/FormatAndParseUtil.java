package org.avni_integration_service.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

public class FormatAndParseUtil {
    // SimpleDateFormat is not thread-safe, and jobs run on a shared scheduler pool: one shared instance returned
    // wrong dates when two organisations' jobs parsed at the same moment. Each thread gets its own, same patterns.
    private static final ThreadLocal<SimpleDateFormat> humanReadableFormat = ThreadLocal.withInitial(() -> new SimpleDateFormat("dd-MM-yyyy"));
    private static final ThreadLocal<SimpleDateFormat> isoDateWithTimezone = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZ"));
    private static final ThreadLocal<SimpleDateFormat> isoDateFormat = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
    private static final ThreadLocal<DateFormat> isoDateTimeFormat = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"));

    public static String escapedForSql(String s) {
        return s.replaceAll("'", "''");
    }

    public static String toISODateStringWithTimezone(Date date) {
        return isoDateWithTimezone.get().format(date);
    }

    public static String toISODateTimeString(Date date) {
        return String.format("%s%s", isoDateTimeFormat.get().format(date), "Z");
    }

    public static String toISODateString(Date date) {
        return isoDateFormat.get().format(date);
    }

    public static String now() {
        return toISODateStringWithTimezone(new Date());
    }

    public static Date fromIsoDate(String dateString) {
        try {
            return isoDateFormat.get().parse(dateString);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static Date fromIsoDateString(String date) {
        try {
            return isoDateTimeFormat.get().parse(date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toHumanReadableFormat(Date date) {
        return humanReadableFormat.get().format(date);
    }

    public static Date parseIsoDateTimeFormat(String dateString) throws ParseException {
        return isoDateTimeFormat.get().parse(dateString);
    }

    public static Date fromAvniDate(String dateString) {
        return FormatAndParseUtil.fromIsoDate(dateString);
    }
    public static Date fromAvniDateTime(String dateString) {
        try {
            return FormatAndParseUtil.parseIsoDateTimeFormat(dateString);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static String fromAvniToOpenMRSDate(String dateString) {
        return FormatAndParseUtil.toISODateTimeString(FormatAndParseUtil.fromAvniDate(dateString));
    }

    public static String fromAvniToOpenMRSGender(String gender) {
        if (Objects.equals(gender, "Male")) return "M";
        if (Objects.equals(gender, "Female")) return "F";
        if (Objects.equals(gender, "Other")) return "U";
        return null;
    }

    public static LocalDateTime toLocalDateTime(Date dateToConvert) {
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
