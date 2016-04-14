package com.thirtyonetensoftware.renamemediatool.filenametester;

import com.thirtyonetensoftware.renamemediatool.support.FilenameTester;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class YearDashMonthDashDay extends FilenameTester {

    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    protected String getPattern() {
        return "\\d{4}-\\d{2}-\\d{2}.*";
    }

    @Override
    protected DateFormat getDateFormat() {
        return mDateFormat;
    }

    @Override
    protected Calendar adjustDate(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        return calendar;
    }
}
