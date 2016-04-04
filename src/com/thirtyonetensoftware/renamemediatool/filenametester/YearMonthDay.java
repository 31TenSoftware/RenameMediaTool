package com.thirtyonetensoftware.renamemediatool.filenametester;

import com.thirtyonetensoftware.renamemediatool.support.FilenameTester;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

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
}
