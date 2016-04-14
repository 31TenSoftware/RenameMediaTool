package com.thirtyonetensoftware.renamemediatool.filenametester;

import com.thirtyonetensoftware.renamemediatool.support.FilenameTester;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class YearMonthDay extends FilenameTester {

    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyyMMdd");

    @Override
    protected String getPattern() {
        return "\\d{8}.*";
    }

    @Override
    protected DateFormat getDateFormat() {
        return mDateFormat;
    }

    @Override
    protected void adjustDate(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 12);
    }
}
