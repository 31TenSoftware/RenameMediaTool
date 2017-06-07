package com.thirtyonetensoftware.renamemediatool;

import com.thirtyonetensoftware.renamemediatool.filenametester.*;
import com.thirtyonetensoftware.renamemediatool.support.FilenameTester;
import javafx.concurrent.Task;
import javafx.scene.control.TextArea;

import java.io.File;
import java.io.FileFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

public class ProcessWorker extends Task<Integer> {

    // ------------------------------------------------------------------------
    // Class Variables
    // ------------------------------------------------------------------------

    private static final int SAME_TIME_SECONDS = 1;

    // changes.csv display format
    private static final SimpleDateFormat mOutputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // ------------------------------------------------------------------------
    // Instance Variables
    // ------------------------------------------------------------------------

    private final File mFile;

    private final boolean mStaggerDateTimes;

    private final ArrayList<MediaItem> mChangeItems;

    private final Controller mController;

    private int mFileCount = 0;

    private final int PROGRESS_LOOPS;

    private int mMaxProgress = 0;

    private int mProgress = 0;

    private final ArrayList<FilenameTester> mFilenameTesters = new ArrayList<>();

    private final MessageConsumer mMessageConsumer;

    private final FileFilter mDirectoryFilter = File::isDirectory;

    private final FileFilter mFileFilter = file -> {
        String path = file.getPath().toLowerCase();
        return path.endsWith(".jpeg") ||
                path.endsWith(".jpg") ||
                path.endsWith(".png") ||
                path.endsWith(".bmp") ||
                path.endsWith(".mp4") ||
                path.endsWith(".mov");
    };

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    public ProcessWorker(Controller controller, TextArea textArea, File file, boolean staggerDateTimes,
                         ArrayList<MediaItem> changeItems) {
        mController = controller;
        mFile = file;

        mStaggerDateTimes = staggerDateTimes;
        PROGRESS_LOOPS = mStaggerDateTimes ? 4 : 3;

        mChangeItems = changeItems;

        textArea.clear();

        mMessageConsumer = new MessageConsumer(textArea);
        mMessageConsumer.start();

        mFilenameTesters.clear();
        mFilenameTesters.add(new YearDashMonthDashDayWithTime());
        mFilenameTesters.add(new YearMonthDayTime());
        // mFilenameTesters.add(new MonthDayYearTime());
        mFilenameTesters.add(new YearDashMonthDashDay());
        mFilenameTesters.add(new YearDashMonth());
        mFilenameTesters.add(new YearMonthDay());
    }

    // ------------------------------------------------------------------------
    // Super Methods
    // ------------------------------------------------------------------------

    @Override
    protected Integer call() throws Exception {
        setup(mFile);

        mMessageConsumer.add("\n\nSCANNING...");

        int result = processDirectory(mFile);

        updateProgress(mFileCount, mFileCount);
        mMessageConsumer.add("\n\nFINISHED");

        if (result == 0) {
            mMessageConsumer.add("\n\n" + result + " issues found. Click button to write changes.");
            mController.enableWriteButton(true);
        } else {
            mMessageConsumer.add("\n\n" + result + " issues found. Fix errors before writing the changes!");
            mController.enableWriteButton(false);
        }

        return result;
    }

    @Override
    protected void cancelled() {
        super.cancelled();

        updateProgress(mFileCount, mFileCount);
        mMessageConsumer.add("\n\nCANCELLED");
    }

    // ------------------------------------------------------------------------
    // Private Methods
    // ------------------------------------------------------------------------

    private void setup(File file) {
        mFileCount = calculateTotalPhotos(file, 0);
        mMaxProgress = mFileCount * PROGRESS_LOOPS;

        mMessageConsumer.add("Number of files: " + String.valueOf(mFileCount));
    }

    private int calculateTotalPhotos(File file, int max) {
        File[] photos = file.listFiles(mFileFilter);
        if (photos != null) {
            max += photos.length;
        }

        File[] directories = file.listFiles(mDirectoryFilter);
        if (directories != null) {
            for (File directory : directories) {
                max = calculateTotalPhotos(directory, max);
            }
        }

        return max;
    }

    private int processDirectory(File file) {
        int result = 0;

        // process any media in the current directory
        result += processFiles(file);

        // process any directories in the current directory
        File[] files = file.listFiles(mDirectoryFilter);
        if (files != null) {
            for (File f : files) {
                result += processDirectory(f);
            }
        }

        return result;
    }

    private int processFiles(File file) {
        int result = 0;

        File[] files = file.listFiles(mFileFilter);
        if (files == null) {
            mMessageConsumer.add("\n\nNO FILES FOUND IN: " + file.getPath());
            result++;
            return result;
        }

        ArrayList<MediaItem> items = new ArrayList<>();
        // for all the media, if we can determine a date/time, add it to the list
        for (File f : files) {
            if (isCancelled()) {
                return result;
            }

            MediaItem item = new MediaItem(f, mFilenameTesters);

            if (!item.determineDateTime()) {
                mMessageConsumer.add("\n" + item.getErrorMessage());
                result++;
            } else {
                items.add(item);
            }

            mProgress++;
            updateProgress(mProgress, mMaxProgress);
        }

        // sort the items (will sort by date/time, then name)
        Collections.sort(items);

        if (mStaggerDateTimes) {
            staggerDateTimes(items);
        }

        // calculate new filenames for the ones with newDateTime
        Calendar calendar = Calendar.getInstance();
        int count = 1, dayOfYear = -1, hourOfDay = -1, minute = -1, second = -1;
        for (MediaItem item : items) {
            calendar.setTime(item.getDateTime());

            if (calendar.get(Calendar.DAY_OF_YEAR) == dayOfYear
                    && calendar.get(Calendar.HOUR_OF_DAY) == hourOfDay
                    && calendar.get(Calendar.MINUTE) == minute
                    && calendar.get(Calendar.SECOND) == second) {
                count++;
            } else {
                dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
                hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
                minute = calendar.get(Calendar.MINUTE);
                second = calendar.get(Calendar.SECOND);
                count = 1;
            }

            // if this returns true, then a file rename will occur
            item.generateNewFilename(count);

            mProgress++;
            updateProgress(mProgress, mMaxProgress);
        }

        // loop through all media, if one will require a new date or filename, print it out
        if (!items.isEmpty()) {
            mMessageConsumer.add("\n" + String.format("%50s  |  %19s  |  %s", "file", "new datetime", "new filename"));
        }
        for (MediaItem item : items) {
            if (isCancelled()) {
                return result;
            }

            if (item.hasNewDateTime() || item.hasNewFilename()) {
                // pad the filepath to at least 50 characters, left-aligned. 52 with the last 2 spaces
                String formattedFilepath = String.format("%-50s  ", item.getFilepath());
                // only show the last 52 characters
                mMessageConsumer.add("\n" + formattedFilepath.substring(formattedFilepath.length() - 52));

                if (item.hasNewDateTime()) {
                    mMessageConsumer.add("|  " + mOutputFormat.format(item.getDateTime()) + "  |");
                } else {
                    // 19 spaces in the middle, for the same length as mOutputFormat
                    mMessageConsumer.add("|                       |");
                }

                if (item.hasNewFilename()) {
                    mMessageConsumer.add("  " + item.getNewFilename());
                }

                mChangeItems.add(item);
            }

            mProgress++;
            updateProgress(mProgress, mMaxProgress);
        }

        return result;
    }

    // recalculate new dates if date/times are the same for sequential files in the list
    private void staggerDateTimes(ArrayList<MediaItem> items) {
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
                // only do 1 second due to issue #1
                for (MediaItem itemWithSameDateTime : itemsWithSameDateTime) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(itemWithSameDateTime.getDateTime());
                    calendar.add(Calendar.SECOND, index * SAME_TIME_SECONDS);

                    itemWithSameDateTime.setDateTime(calendar.getTime());

                    index++;
                }
            }

            mProgress++;
            updateProgress(mProgress, mMaxProgress);
        }
    }
}
