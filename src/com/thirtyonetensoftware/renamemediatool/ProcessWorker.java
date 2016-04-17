package com.thirtyonetensoftware.renamemediatool;

import com.thirtyonetensoftware.renamemediatool.filenametester.YearDashMonth;
import com.thirtyonetensoftware.renamemediatool.filenametester.YearDashMonthDashDay;
import com.thirtyonetensoftware.renamemediatool.filenametester.YearMonthDay;
import com.thirtyonetensoftware.renamemediatool.support.FilenameTester;
import javafx.concurrent.Task;
import javafx.scene.control.TextArea;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

public class ProcessWorker extends Task<Integer> {

    // ------------------------------------------------------------------------
    // Class Variables
    // ------------------------------------------------------------------------

    private static final int PROGRESS_LOOPS = 4;

    // changes.csv display format
    private static final SimpleDateFormat mOutputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    // ------------------------------------------------------------------------
    // Instance Variables
    // ------------------------------------------------------------------------

    private final File mFile;

    private File mChangesFile;

    private final ArrayList<MediaItem> mChangeItems;

    private final Controller mController;

    private int mFileCount = 0;

    private int mMaxProgress = 0;

    private int mProgress = 0;

    private final ArrayList<FilenameTester> mFilenameTesters = new ArrayList<>();

    private final MessageConsumer mMessageConsumer;

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

    public ProcessWorker(Controller controller, TextArea textArea, File file, ArrayList<MediaItem> changeItems) {
        mController = controller;
        mFile = file;
        mChangeItems = changeItems;

        textArea.clear();

        mMessageConsumer = new MessageConsumer(textArea);
        mMessageConsumer.start();

        mChangesFile = new File(mFile.getPath() + File.separator + "changes.csv");
        try {
            mChangesFile.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(mChangesFile));
            writer.write("file,newDateTime,newFilename");
            writer.close();
        } catch (IOException e) {
            mMessageConsumer.add("\nCHANGES FILE COULD NOT BE CREATED: " + e.getMessage());
            mChangesFile = null;
        }

        mFilenameTesters.clear();
        mFilenameTesters.add(new YearDashMonthDashDay());
        mFilenameTesters.add(new YearDashMonth());
        mFilenameTesters.add(new YearMonthDay());
    }

    // ------------------------------------------------------------------------
    // Super Methods
    // ------------------------------------------------------------------------

    @Override
    protected Integer call() throws Exception {
        if (mChangesFile == null) {
            return 1;
        }

        setup(mFile);

        mMessageConsumer.add("\n\nSTARTING\n\nErrors:");

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
            mMessageConsumer.add("\nNO FILES FOUND IN: " + file.getPath());
            result++;
            return result;
        }

        ArrayList<MediaItem> items = new ArrayList<>();
        // for all the images, if we can determine a date/time, add it to the list
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

        // we used to recalculate new dates if date/times are the same for sequential files in the list
        // now we just add an extra count to the end of the filename
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
                // set the count on each MediaItem that have the same date/time
                for (MediaItem itemWithSameDateTime : itemsWithSameDateTime) {
                    index += 1;
                    itemWithSameDateTime.setMatchingCount(index);
                }

                // important to move the itemsIndex past this grouping of matching date/times
                itemsIndex += itemsWithSameDateTime.size();
            }

            mProgress++;
            updateProgress(mProgress, mMaxProgress);
        }

        // calculate new filenames for the ones with newDateTime
        Calendar calendar = Calendar.getInstance();
        int count = 1, dayOfYear = -1;
        for (MediaItem item : items) {
            calendar.setTime(item.getDateTime());

            if (calendar.get(Calendar.DAY_OF_YEAR) == dayOfYear) {
                count++;
            } else {
                dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
                count = 1;
            }

            // if this returns true, then a file rename will occur
            if (item.generateNewFilename(count)) {
                File renameFile = new File(item.getNewFilename());

                if (renameFile.exists()) {
                    mMessageConsumer.add("\nRENAME WILL FAIL: " + item.getFile().getName() + " to -> " + item.getNewFilename());
                    result++;
                }
            }

            mProgress++;
            updateProgress(mProgress, mMaxProgress);
        }

        // loop through all images, if one will require a new date or filename, print it out
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(mChangesFile, true));
            for (MediaItem item : items) {
                if (isCancelled()) {
                    return result;
                }

                if (item.hasNewDateTime() || item.hasNewFilename()) {
                    writer.newLine();
                    writer.append(item.getFile().getPath()).append(",");

                    if (item.hasNewDateTime()) {
                        writer.append(mOutputFormat.format(item.getNewDateTime())).append(",");
                    } else {
                        writer.append(",");
                    }

                    if (item.hasNewFilename()) {
                        writer.append(item.getNewFilename());
                    }

                    mChangeItems.add(item);
                }

                mProgress++;
                updateProgress(mProgress, mMaxProgress);
            }
            writer.close();
        } catch (IOException e) {
            mMessageConsumer.add("\nCANNOT WRITE CHANGES TO changes.csv: " + e.getMessage());
            result++;
        }

        return result;
    }
}
