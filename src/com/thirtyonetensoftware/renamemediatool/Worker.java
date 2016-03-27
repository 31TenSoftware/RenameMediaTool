package com.thirtyonetensoftware.renamemediatool;

import javafx.concurrent.Task;
import javafx.scene.control.TextArea;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

public class Worker extends Task<Integer> {

    // ------------------------------------------------------------------------
    // Class Variables
    // ------------------------------------------------------------------------

    private static final int PROGRESS_LOOPS = 3;

    // EXIF Date/Time format
    private static final SimpleDateFormat mOutputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // ------------------------------------------------------------------------
    // Instance Variables
    // ------------------------------------------------------------------------

    private final File mFile;

    private int mFileCount = 0;

    private int mMaxProgress = 0;

    private int mProgress = 0;

    private MessageConsumer mMessageConsumer;

    private final FileFilter mDirectoryFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            return file.isDirectory();
        }
    };

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

        textArea.clear();

        mMessageConsumer = new MessageConsumer(textArea);
        mMessageConsumer.start();
    }

    // ------------------------------------------------------------------------
    // Super Methods
    // ------------------------------------------------------------------------

    @Override
    protected Integer call() throws Exception {
        setup(mFile);

        mMessageConsumer.add("\nSTARTING\n");

        int result = processDirectory(mFile);

        updateProgress(mFileCount, mFileCount);
        mMessageConsumer.add("\n\nFINISHED\n", false);

        return result;
    }

    @Override
    protected void cancelled() {
        super.cancelled();

        updateProgress(mFileCount, mFileCount);
        mMessageConsumer.add("\nCANCELLED\n");
    }

    // ------------------------------------------------------------------------
    // Private Methods
    // ------------------------------------------------------------------------

    private void setup(File file) {
        mMessageConsumer.add("Determining number of files... ", false);

        mFileCount = calculateTotalPhotos(file, 0);
        mMaxProgress = mFileCount * PROGRESS_LOOPS;

        mMessageConsumer.add(String.valueOf(mFileCount), false);
    }

    private int calculateTotalPhotos(File file, int max) {
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

    private int processDirectory(File file) {
        File[] files = file.listFiles(mDirectoryFilter);
        int result = 0;

        if (files != null) {
            for (File f : files) {
                result += processFiles(f);

                result += processDirectory(f);
            }
        }

        return result;
    }

    private int processFiles(File file) {
        int result = 0;

        File[] files = file.listFiles(mFileFilter);
        if (files == null) {
            mMessageConsumer.add("\nNO FILES FOUND IN: " + file.getPath() + "\n");
            result++;
            return result;
        }

        ArrayList<MediaItem> items = new ArrayList<>();
        // for all the images, if we can determine a date/time, add it to the list
        for (File f : files) {
            if (isCancelled()) {
                return result;
            }

            MediaItem item = new MediaItem(f);

            if (!item.determineDateTime()) {
                mMessageConsumer.add("\n" + item.getErrorMessage() + "\n");
                result++;
            } else {
                items.add(item);
            }

            mProgress++;
            updateProgress(mProgress, mMaxProgress);
        }

        // sort the items (will sort by date/time, then name)
        Collections.sort(items);

        // recalculate new dates if date/times are the same for sequential files in the list
        for (int itemsIndex = 0; itemsIndex < items.size(); itemsIndex++) {
            if (itemsIndex != items.size() - 1 &&
                    items.get(itemsIndex).getDateTime().equals(items.get(itemsIndex + 1).getDateTime())) {
                MediaItem item = items.get(itemsIndex);
                ArrayList<MediaItem> itemsWithSameDateTime = new ArrayList<>();

                for (int subItemsIndex = itemsIndex; subItemsIndex < items.size(); subItemsIndex++) {
                    if (items.get(subItemsIndex).getDateTime().equals(item.getDateTime())) {
                        itemsWithSameDateTime.add(items.get(subItemsIndex));
                    }
                }

                Collections.sort(itemsWithSameDateTime);

                int index = 0;
                // get the date, add 5 seconds * index and set the date
                for (MediaItem itemWithSameDateTime : itemsWithSameDateTime) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(itemWithSameDateTime.getDateTime());
                    calendar.add(Calendar.SECOND, index * 5);

                    itemWithSameDateTime.setDateTime(calendar.getTime());

                    index++;
                }
            }

            mProgress++;
            updateProgress(mProgress, mMaxProgress);
        }

        // TODO loop through the media items and calculate the new filenames for the ones with newDateTime

        // loop through all images, if one will require a new date or filename, print it out
        for (MediaItem item : items) {
            if (isCancelled()) {
                return result;
            }

            if (item.hasNewDateTime()) {
                mMessageConsumer.add(item.getFile().getName() + " NEW DATE: " + mOutputFormat.format(item.getNewDateTime()));
            }

            if (item.hasNewFilename()) {
                mMessageConsumer.add(item.getFile().getName() + " NEW FILENAME: " + item.getNewFilename());
            }

            mProgress++;
            updateProgress(mProgress, mMaxProgress);
        }

        return result;
    }

    // TODO write a method that on a button click writes the new EXIF and filenames to disk
}
