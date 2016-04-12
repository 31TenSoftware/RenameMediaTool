package com.thirtyonetensoftware.renamemediatool.filenametester;

import com.thirtyonetensoftware.renamemediatool.support.FilenameTester;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class YearMonthDay implements FilenameTester {

    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyyMMdd");

    @Override
    public String getPattern() {
        return "\\d{8}.*";
    }

    @Override
    public DateFormat getDateFormat() {
        return mDateFormat;
    }
}
