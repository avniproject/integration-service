package org.avni_integration_service.goonj.util;

import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateTimeUtil {

    public static final String IST = "Asia/Kolkata";
    public static String UTC = "UTC";
    public static final String adhocTaskDateFormatDTOPattern = "yyyy-MM-dd hh:mm:ss";
    private static final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final SimpleDateFormat adhocDateFormater = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private static final SimpleDateFormat avniSimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private static final SimpleDateFormat simpleDateFormatInventory = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final SimpleDateFormat goonjDateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private static final SimpleDateFormat goonjRequestDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    // Todo: Enforce single format for all DateTime entries from goonj response!
    private static final String goonjRequestDateFormatRegex = "^\\d{4}\\-(0[1-9]|1[012])\\-(0[1-9]|[12][0-9]|3[01])$";
    private static final String goonjDateFormatRegex = "^(0[1-9]|[12][0-9]|3[01])\\/(0[1-9]|1[012])\\/\\d{4}$";
    private static final String simpleDateFormatRegex = "^(19|20)\\d\\d[-](0[1-9]|1[0-2])[-](0[1-9]|1[0-9]|2[0-9]|3[01])[T](0[0-9]|1[0-9]|2[0123])[:](0[0-9]|[12345][0-9])[:](0[0-9]|[12345][0-9])$";
    private static final String simpleDateFormatInventoryRegex = "^(19|20)\\d\\d[-](0[1-9]|1[0-2])[-](0[1-9]|1[0-9]|2[0-9]|3[01])[T](0[0-9]|1[0-9]|2[0123])[:](0[0-9]|[12345][0-9])[:](0[0-9]|[12345][0-9])[.]([0-9][0-9][0-9])[Z]$";
    private static final String avniSimpleDateFormatRegex = "^(19|20)\\d\\d[-](0[1-9]|1[0-2])[-](0[1-9]|1[0-9]|2[0-9]|3[01])[T](0[0-9]|1[0-9]|2[0123])[:](0[0-9]|[12345][0-9])[:](0[0-9]|[12345][0-9])[.]([0-9][0-9][0-9])$";
    private static final String goonjFilterDateFormatRegx = "^[A-Za-z]{3} [A-Za-z]{3} \\d{2} \\d{2}:\\d{2}:\\d{2} [A-Za-z]{3} \\d{4}$";
    private static final Jsr310JpaConverters.LocalDateTimeConverter ldtc = new Jsr310JpaConverters.LocalDateTimeConverter();


    public static String formatDateTime(Date localDateTime) {
        return simpleDateFormat.format(localDateTime);
    }

    public static Date convertToDate(String localDateTime) {
        return getDate(localDateTime);
    }

    public static Date getDate(String localDateTime) {
        if (localDateTime.matches(simpleDateFormatRegex)) {
            try {
                return simpleDateFormat.parse(localDateTime);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        if (localDateTime.matches(simpleDateFormatInventoryRegex)) {
            try {
                return simpleDateFormatInventory.parse(localDateTime);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        if (localDateTime.matches(goonjRequestDateFormatRegex)) {
            try {
                return goonjRequestDateFormat.parse(localDateTime);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        if (localDateTime.matches(goonjDateFormatRegex)) {
            try {
                return goonjDateFormat.parse(localDateTime);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        if (localDateTime.matches(avniSimpleDateFormatRegex)) {
            try {
                return avniSimpleDateFormat.parse(localDateTime);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
        if (localDateTime.matches(goonjFilterDateFormatRegx)){
            try{
                return adhocDateFormater.parse(localDateTime);
            }
            catch (ParseException e){
                throw  new RuntimeException(e);
            }
        }
        return null;
    }

    public static Date convertToDateFromGoonjDateString(String goonjDateString) {
        return getDate(goonjDateString);
    }

    public static String formatDateTime(String goonjDateString) {
        try {
            Date date = goonjDateFormat.parse(goonjDateString);
            return dateTimeFormat.format(ldtc.convertToEntityAttribute(date));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static String formatDate(Date encounterDateTime) {
        return goonjRequestDateFormat.format(encounterDateTime);
    }

    public static Date offsetTimeZone(Date date, String fromTZ, String toTZ){

        // Construct FROM and TO TimeZone instances
        TimeZone fromTimeZone = TimeZone.getTimeZone(fromTZ);
        TimeZone toTimeZone = TimeZone.getTimeZone(toTZ);

        // Get a Calendar instance using the default time zone and locale.
        Calendar calendar = Calendar.getInstance();

        // Set the calendar's time with the given date
        calendar.setTimeZone(fromTimeZone);
        calendar.setTime(date);

        System.out.println("Input: " + calendar.getTime() + " in " + fromTimeZone.getDisplayName());

        // FROM TimeZone to UTC
        calendar.add(Calendar.MILLISECOND, fromTimeZone.getRawOffset() * -1);

        if (fromTimeZone.inDaylightTime(calendar.getTime())) {
            calendar.add(Calendar.MILLISECOND, calendar.getTimeZone().getDSTSavings() * -1);
        }

        // UTC to TO TimeZone
        calendar.add(Calendar.MILLISECOND, toTimeZone.getRawOffset());

        if (toTimeZone.inDaylightTime(calendar.getTime())) {
            calendar.add(Calendar.MILLISECOND, toTimeZone.getDSTSavings());
        }

        return calendar.getTime();
    }
}
