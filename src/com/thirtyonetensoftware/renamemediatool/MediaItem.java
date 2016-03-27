package com.thirtyonetensoftware.renamemediatool;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.constants.ExifTagConstants;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

public class MediaItem implements Comparable<MediaItem> {

    // ------------------------------------------------------------------------
    // Instance Variables
    // ------------------------------------------------------------------------

    // EXIF Date/Time format
    private static final SimpleDateFormat mFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

    // Potential filename Date/Time format 1
    private static final SimpleDateFormat mFilenameFormat1 = new SimpleDateFormat("yyyy-MM");

    // Potential filename Date/Time format 2
    private static final SimpleDateFormat mFilenameFormat2 = new SimpleDateFormat("yyyy-MM-dd");

    // Potential filename Date/Time format 3
    private static final SimpleDateFormat mFilenameFormat3 = new SimpleDateFormat("yyyyMMdd");

    private final File mFile;

    private Date mOriginalDateTime;

    private Date mNewDateTime;

    private String mNewFilename;

    private String mErrorMessage;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    public MediaItem(File file) {
        mFile = file;
    }

    // ------------------------------------------------------------------------
    // Comparable Interface
    // ------------------------------------------------------------------------

    @Override
    public int compareTo(MediaItem item) {
        int distance = 0;

        // first compare by date
        Date thisDateTime = getDateTime();
        Date thatDateTime = item.getDateTime();

        if (thisDateTime == null && thatDateTime == null) {
            distance = 0;
        } else if (thisDateTime != null) {
            distance = thisDateTime.compareTo(thatDateTime);
        }

        // if dates are the same, compare by filename
        if (distance == 0) {
            distance = mFile.getName().compareTo(item.getFile().getName());
        }

        return distance;
    }

    // ------------------------------------------------------------------------
    // Public Methods
    // ------------------------------------------------------------------------

    public String getErrorMessage() {
        return mErrorMessage;
    }

    public File getFile() {
        return mFile;
    }

    public Date getDateTime() {
        return mNewDateTime != null ? mNewDateTime : mOriginalDateTime;
    }

    public void setDateTime(Date dateTime) {
        mNewDateTime = dateTime;
    }

    public Date getNewDateTime() {
        return mNewDateTime;
    }

    public boolean hasNewDateTime() {
        return mNewDateTime != null;
    }

    public String getNewFilename() {
        return mNewFilename;
    }

    public boolean hasNewFilename() {
        return mNewFilename != null;
    }

    public boolean determineDateTime() {
        try {
            // attempt to read the EXIF data
            IImageMetadata metadata = Sanselan.getMetadata(mFile);
            if (metadata instanceof JpegImageMetadata) {
                JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
                // read the date/time tag out
                TiffField dateTimeValue = jpegMetadata.findEXIFValue(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);

                // if there's no date/time value, try to determine one from the file's name
                if (dateTimeValue == null) {
                    mNewDateTime = parseFilenameForDateTime(mFile);
                } else {
                    mOriginalDateTime = mFormat.parse(dateTimeValue.getStringValue().trim());
                }
            }
            // image is a .png or .bmp
            else {
                mOriginalDateTime = new Date(mFile.lastModified());
            }

            if (mNewDateTime == null && mOriginalDateTime == null) {
                mErrorMessage = "\n\nCOULD NOT DETERMINE DATE/TIME FOR " + mFile.getPath() + "\n";
                return false;
            } else {
                return true;
            }
        } catch (ImageReadException | IOException | ParseException e) {
            mErrorMessage = "\n\n" + mFile.getPath() + " ERROR: " + e.getMessage() + "\n";
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // Private Methods
    // ------------------------------------------------------------------------

    private Date parseFilenameForDateTime(File file) {
        String filename = file.getName();
        Date hello;

        try {
            hello = mFilenameFormat2.parse(file.getName());
            if (Pattern.matches("\\d{4}-\\d{2}-\\d{2}.*", filename)) {
                return mFilenameFormat2.parse(file.getName());
            }
        } catch (ParseException e) {

        }

        try {
            hello = mFilenameFormat3.parse(file.getName());
            if (Pattern.matches("\\d{8}.*", filename)) {
                return mFilenameFormat3.parse(file.getName());
            }
        } catch (ParseException e) {

        }

        try {
            hello = mFilenameFormat1.parse(file.getName());
            if (Pattern.matches("\\d{4}-\\d{2}.*", filename)) {
                return mFilenameFormat1.parse(file.getName());
            }
        } catch (ParseException e) {

        }

        return null;
    }
}
