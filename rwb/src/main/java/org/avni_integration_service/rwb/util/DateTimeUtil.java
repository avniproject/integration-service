package org.avni_integration_service.rwb.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

public class DateTimeUtil {
    // TODO: 20/09/23  If needed we have to change date type
    //  public static final String REGISTRATION_DATE = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS";
    public static final String DATE_TIME = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String DD_MM_YYYY = "dd-MM-yyyy";

    public static Date toDate(String dateString, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            Date date = sdf.parse(dateString);
            return date;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static LocalDate toLocalDate(String dateString, String format) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
        try {
            LocalDate date = LocalDate.parse(dateString, dtf);
            return date;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean differenceWithNowLessThanInterval(Date date, int interval, int unit) {
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(date);
        Date startTime = startCalendar.getTime();

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.setTime(new Date());
        endCalendar.add(unit, -interval);
        Date endTime = endCalendar.getTime();

        return startTime.after(endTime);
    }
}
