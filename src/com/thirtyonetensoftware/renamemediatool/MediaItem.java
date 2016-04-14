package com.thirtyonetensoftware.renamemediatool;

import com.thirtyonetensoftware.renamemediatool.support.IFilenameTester;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
import org.apache.sanselan.formats.tiff.TiffField;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.constants.ExifTagConstants;
import org.apache.sanselan.formats.tiff.write.TiffOutputDirectory;
import org.apache.sanselan.formats.tiff.write.TiffOutputField;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class MediaItem implements Comparable<MediaItem> {

    // ------------------------------------------------------------------------
    // Class Variables
    // ------------------------------------------------------------------------

    // EXIF Date/Time format
    private static final SimpleDateFormat mFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

    // Filename format
    private static final SimpleDateFormat mFilenameFormat = new SimpleDateFormat("yyyy-MM-dd_");

    private final File mFile;

    private ArrayList<IFilenameTester> mFilenameTesters = new ArrayList<>();

    private Date mOriginalDateTime;

    private Date mNewDateTime;

    private String mNewFilename;

    private String mErrorMessage;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    public MediaItem(File file, ArrayList<IFilenameTester> testers) {
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

    public boolean generateNewFilename(int count) {
        String name = mFilenameFormat.format(getDateTime());

        name = name + (String.format("%04d", count));

        if (name.equals(mFile.getName())) {
            return false;
        } else {
            mNewFilename = name;
            return true;
        }
    }

    public boolean hasNewFilename() {
        return mNewFilename != null;
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

            TiffOutputDirectory exif = outputSet.getOrCreateExifDirectory();
            exif.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
            TiffOutputField field = TiffOutputField.create(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL,
                    outputSet.byteOrder, mFormat.format(getNewDateTime()));
            exif.add(field);

            saveExifToFile(mFile, outputSet);
        }
        // image is a .png or .bmp
        else {
            mOriginalDateTime = new Date(mFile.lastModified());
        }
    }

    public void commitNewFilename() throws IOException {
        Path path = Paths.get(mFile.toURI());
        Files.move(path, path.resolveSibling(getNewFilename()), REPLACE_EXISTING);
    }

    // ------------------------------------------------------------------------
    // Private Methods
    // ------------------------------------------------------------------------

    private Date parseFilenameForDateTime(File file) {
        String filename = file.getName();
        Date date;

        for (IFilenameTester tester : mFilenameTesters) {
            if ((date = parseFilenameForDateTime(tester, filename)) != null) {
                return date;
            }
        }

        return null;
    }

    private Date parseFilenameForDateTime(IFilenameTester tester, String filename) {
        try {
            if (Pattern.matches(tester.getPattern(), filename)) {
                return tester.getDateFormat().parse(filename);
            }
        } catch (ParseException e) {
            // do nothing
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
