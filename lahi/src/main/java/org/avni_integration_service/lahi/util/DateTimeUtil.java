package org.avni_integration_service.lahi.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeUtil {
    // TODO: 20/09/23  If needed we have to change date type
    //  public static final String REGISTRATION_DATE = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS";
    public static final String REGISTRATION_DATE = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String DATE_OF_BIRTH = "dd-MM-yyyy";

    public static Date toDate(String dateString,String format){
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            Date date = sdf.parse(dateString);
            return date;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Date registrationDate(String dateString){
        return toDate(dateString,REGISTRATION_DATE);
    }

    public static Date dateOfBirth(String dateString){
        return toDate(dateString,DATE_OF_BIRTH);
    }
}
