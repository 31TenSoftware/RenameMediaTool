package com.thirtyonetensoftware.renamemediatool;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.scene.control.TextArea;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

public class Worker extends Task<Integer> {

    // ------------------------------------------------------------------------
    // Instance Variables
    // ------------------------------------------------------------------------

    private final File mFile;

    private double mMaxProgress = 0;

    private final SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aa");

    private final FileFilter mFileFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            String path = file.getPath().toLowerCase();
            return path.endsWith(".jpeg") ||
                    path.endsWith(".jpg") ||
                    path.endsWith(".png") ||
                    path.endsWith(".bmp") ||
                    file.isDirectory();
        }
    };

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    public Worker(final TextArea textArea, File file) {
        mFile = file;

        messageProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                textArea.appendText(t1);
            }
        });
    }

    // ------------------------------------------------------------------------
    // Super Methods
    // ------------------------------------------------------------------------

    @Override
    protected Integer call() throws Exception {
        setup(mFile);

        updateMessage("\nSTARTING\n\n");

        int result = traverse(mFile);

        updateMessage("\nFINISHED\n");

        return result;
    }

    @Override
    protected void cancelled() {
        super.cancelled();

        updateMessage("\nCANCELLED\n");
    }

    // ------------------------------------------------------------------------
    // Private Methods
    // ------------------------------------------------------------------------

    private void setup(File file) {
        updateMessage("Determining number of files...\n");

        mMaxProgress = calculateTotalPhotos(file, 0);
    }

    private double calculateTotalPhotos(File file, double max) {
        File[] photosAndFolders = file.listFiles(mFileFilter);
        if (photosAndFolders != null) {
            max += photosAndFolders.length;

            for (File f : photosAndFolders) {
                if (f.isDirectory()) {
                    max--;
                    max = calculateTotalPhotos(f, max);
                }
            }
        }

        return max;
    }

    // pseudocode for renaming all the image files and setting the correct exif data

    // first only worry about getting the date/time of the files correct
    // because then it is trivial to sort by date/time (none will have the same date_time anymore), and rename

    private int traverse(File file) {
        File[] files = file.listFiles();

        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    traverse(f);
                }
            }

            for (File f : files) {
                if (!f.isDirectory() && !isCancelled()) {
                    try {
                        Metadata metadata = ImageMetadataReader.readMetadata(f);

                        ExifSubIFDDirectory exif
                                = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

                        Date date;
                        if (exif != null) {
                            date = exif.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);

                            if (date == null) {
                                date = new Date(f.lastModified());

                                updateMessage("NEW DATETIME TAG: " + f.getName() + " -- " + mFormat.format(date) + "\n");
                            }
                        } else {
                            date = new Date(f.lastModified());
                            updateMessage("NO EXIF FOR: " + f.getName() + " --> " + mFormat.format(date) + "\n");
                        }
                    } catch (ImageProcessingException | IOException e) {
                        updateMessage(f.getName() + " error " + "\n");
                        return -1;
                    }
                }
            }
        }

        return 0;
    }
}
