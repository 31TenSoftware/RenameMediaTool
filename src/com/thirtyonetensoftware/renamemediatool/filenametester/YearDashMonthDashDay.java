package com.thirtyonetensoftware.renamemediatool.filenametester;

import com.thirtyonetensoftware.renamemediatool.support.IFilenameTester;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class YearDashMonthDashDay implements IFilenameTester {

    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public String getPattern() {
        return "\\d{4}-\\d{2}-\\d{2}.*";
    }

    @Override
    public DateFormat getDateFormat() {
        return mDateFormat;
    }
}
