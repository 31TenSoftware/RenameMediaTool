package com.thirtyonetensoftware.renamemediatool.support;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.regex.Pattern;

public abstract class FilenameTester {

    protected abstract String getPattern();

    protected abstract DateFormat getDateFormat();

    public Date parseFilenameForDateTime(String filename) {
        try {
            if (Pattern.matches(getPattern(), filename)) {
                return getDateFormat().parse(filename);
            }
        } catch (ParseException e) {
            // do nothing
        }

        return null;
    }
}
