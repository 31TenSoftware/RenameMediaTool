package com.thirtyonetensoftware.renamemediatool.filenametester;

import com.thirtyonetensoftware.renamemediatool.support.FilenameTester;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

@SuppressWarnings("unused")
public class MonthDayYearTime extends FilenameTester {

    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("MMddyyyy_HHmmss");

    @Override
    protected String getPattern() {
        return "\\d{8}_\\d{6}_.*";
    }

    @Override
    protected DateFormat getDateFormat() {
        return mDateFormat;
    }
}
