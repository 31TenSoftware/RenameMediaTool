package com.thirtyonetensoftware.renamemediatool.filenametester;

import com.thirtyonetensoftware.renamemediatool.support.FilenameTester;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class YearDashMonth implements FilenameTester {

    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM");

    @Override
    public String getPattern() {
        return "\\d{4}-\\d{2}.*";
    }

    @Override
    public DateFormat getDateFormat() {
        return mDateFormat;
    }
}
