package com.thirtyonetensoftware.renamemediatool;

import com.thirtyonetensoftware.renamemediatool.support.FilenameTester;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.constants.ExifTagConstants;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.apache.sanselan.formats.tiff.write.TiffOutputDirectory;
import org.apache.sanselan.formats.tiff.write.TiffOutputField;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class MediaItem implements Comparable<MediaItem> {

    // ------------------------------------------------------------------------
    // Class Variables
    // ------------------------------------------------------------------------

    // EXIF Date/Time format
    private static final SimpleDateFormat mFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

    // Filename format
    private static final SimpleDateFormat mFilenameFormat = new SimpleDateFormat("yyyy-MM-dd_");

    private File mFile;

    private ArrayList<FilenameTester> mFilenameTesters = new ArrayList<>();

    private Date mOriginalDateTime;

    private Date mNewDateTime;

    private String mNewFilename;

    private String mTempName;

    private String mErrorMessage;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    public MediaItem(File file, ArrayList<FilenameTester> testers) {
        mFile = file;
        mFilenameTesters = testers;
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
            distance = mFile.getName().compareToIgnoreCase(mFile.getName());
        }

        return distance;
    }

    // ------------------------------------------------------------------------
    // Public Methods
    // ------------------------------------------------------------------------

    public String getErrorMessage() {
        return mErrorMessage;
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

    public boolean determineDateTime() {
        try {
            String filename = mFile.getName().toLowerCase();

            if (filename.endsWith(".jpeg") | filename.endsWith(".jpg")) {
                TiffField dateTimeValue = null;

                // attempt to read the EXIF data
                IImageMetadata metadata = Sanselan.getMetadata(mFile);
                if (metadata instanceof JpegImageMetadata) {
                    JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
                    // read the date/time tag out
                    dateTimeValue = jpegMetadata.findEXIFValue(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);

                    if (dateTimeValue == null) {
                        dateTimeValue = jpegMetadata.findEXIFValue(ExifTagConstants.EXIF_TAG_CREATE_DATE);
                    }
                }

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

    public void commitNewDateTime() throws IOException, ImageReadException, ImageWriteException {
        String filename = mFile.getName().toLowerCase();

        if (filename.endsWith(".jpeg") | filename.endsWith(".jpg")) {
            TiffOutputSet outputSet = new TiffOutputSet();

            // attempt to read the EXIF data
            IImageMetadata metadata = Sanselan.getMetadata(mFile);
            if (metadata instanceof JpegImageMetadata) {
                JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;

                TiffImageMetadata exif = jpegMetadata.getExif();
                if (exif != null) {
                    outputSet = exif.getOutputSet();
                }
            }

            String newDateTimeValue = mFormat.format(getNewDateTime());

            TiffOutputField new_date_time_orig_field = new TiffOutputField(TiffConstants.EXIF_TAG_DATE_TIME_ORIGINAL,
                    TiffConstants.FIELD_TYPE_ASCII,
                    newDateTimeValue.length(),
                    newDateTimeValue.getBytes());
            TiffOutputField new_create_date_field = new TiffOutputField(TiffConstants.EXIF_TAG_CREATE_DATE,
                    TiffConstants.FIELD_TYPE_ASCII,
                    newDateTimeValue.length(),
                    newDateTimeValue.getBytes());

            TiffOutputDirectory exif = outputSet.getOrCreateExifDirectory();
            exif.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
            exif.removeField(ExifTagConstants.EXIF_TAG_CREATE_DATE);
            exif.add(new_date_time_orig_field);
            exif.add(new_create_date_field);

            saveExifToFile(mFile, outputSet);
        }
        // image is a .png or .bmp
        else {
            mOriginalDateTime = new Date(mFile.lastModified());
        }
    }

    public String getNewFilename() {
        return mNewFilename;
    }

    public boolean hasNewFilename() {
        return mNewFilename != null;
    }

    public void generateNewFilename(int count) {
        String currentName = mFile.getName();

        int dot = currentName.lastIndexOf(".");
        String extension = currentName.substring(dot).toLowerCase();

        String newBaseFilename = mFilenameFormat.format(getDateTime()) +
                (String.format("%04d", count));

        String newFilename = newBaseFilename + extension;

        if (!newFilename.equals(currentName)) {
            mTempName = newBaseFilename + UUID.randomUUID() + extension + ".temp";

            mNewFilename = newFilename;
        }
    }

    public void commitTempFilename() {
        Path path = Paths.get(mFile.toURI());
        File tempFile = path.resolveSibling(mTempName).toFile();

        mFile.renameTo(tempFile);
        // update mFile to point to the new File
        mFile = tempFile;
    }

    public void commitNewFilename() {
        Path path = Paths.get(mFile.toURI());
        File newFile = path.resolveSibling(mNewFilename).toFile();

        mFile.renameTo(newFile);
        // update mFile to point to the new File
        mFile = newFile;
    }

    public String getFilepath() {
        return mFile == null ? null : mFile.getPath();
    }

    // ------------------------------------------------------------------------
    // Private Methods
    // ------------------------------------------------------------------------

    private Date parseFilenameForDateTime(File file) {
        String filename = file.getName();
        Date date;

        for (FilenameTester tester : mFilenameTesters) {
            if ((date = tester.parseFilenameForDateTime(filename)) != null) {
                return date;
            }
        }

        return null;
    }

    private void saveExifToFile(File imageFile, TiffOutputSet exif)
            throws IOException, ImageWriteException, ImageReadException {
        String tempFileName = imageFile.getAbsolutePath() + ".tmp";
        File tempFile = new File(tempFileName);

        BufferedOutputStream tempStream = new BufferedOutputStream(new FileOutputStream(tempFile));
        new ExifRewriter().updateExifMetadataLossless(imageFile, tempStream, exif);
        tempStream.close();

        if (imageFile.delete()) {
            tempFile.renameTo(imageFile);
        }
    }
}
