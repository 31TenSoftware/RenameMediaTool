package com.thirtyonetensoftware.renamemediatool.support;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

public abstract class FilenameTester {

    protected abstract String getPattern();

    protected abstract DateFormat getDateFormat();

    protected Calendar adjustDate(Calendar calendar) {
        return calendar;
    }

    public final Date parseFilenameForDateTime(String filename) {
        try {
            if (Pattern.matches(getPattern(), filename)) {
                Date date = getDateFormat().parse(filename);

                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                adjustDate(calendar);

                return calendar.getTime();
            }
        } catch (ParseException e) {
            // do nothing
        }

        return null;
    }
}
