package com.thirtyonetensoftware.renamemediatool;

import javafx.concurrent.Task;
import javafx.scene.control.TextArea;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;

import java.io.IOException;
import java.util.ArrayList;

public class CommitWorker extends Task<Integer> {

    // ------------------------------------------------------------------------
    // Class Variables
    // ------------------------------------------------------------------------

    private static final int PROGRESS_LOOPS = 3;

    // ------------------------------------------------------------------------
    // Instance Variables
    // ------------------------------------------------------------------------

    private final ArrayList<MediaItem> mChangeItems;

    private final MessageConsumer mMessageConsumer;

    private final int mTotalIterations;

    private int mIterations;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    public CommitWorker(TextArea textArea, ArrayList<MediaItem> changeItems) {
        mChangeItems = changeItems;

        mTotalIterations = mChangeItems.size() * PROGRESS_LOOPS;

        updateProgress(0, mTotalIterations);

        mMessageConsumer = new MessageConsumer(textArea);
        mMessageConsumer.start();
    }

    // ------------------------------------------------------------------------
    // Super Methods
    // ------------------------------------------------------------------------

    @Override
    protected Integer call() throws Exception {
        int result = 0;

        mMessageConsumer.add("\n\nCOMMITTING NEW DATE/TIME VALUES:");

        result += commitNewDateTimes();

        if (result > 0) {
            mMessageConsumer.add("\n\n" + result + " issues writing new date times. Not renaming files until resolved.");
            return result;
        }

        mMessageConsumer.add("\n\nCOMMITTING NEW FILENAMES:");

        result += commitTempFilenames();

        result += commitNewFilenames();

        updateProgress(mTotalIterations, mTotalIterations);
        mMessageConsumer.add("\n\nFINISHED. " + result + " issues.");

        return result;
    }

    // ------------------------------------------------------------------------
    // Private Methods
    // ------------------------------------------------------------------------

    private int commitNewDateTimes() {
        int result = 0;

        for (MediaItem item : mChangeItems) {
            if (isCancelled()) {
                return result;
            }

            if (item.hasNewDateTime()) {
                try {
                    item.commitNewDateTime();
                } catch (IOException | ImageReadException | ImageWriteException e) {
                    mMessageConsumer.add("\n" + e);
                    result++;
                }
            }

            mIterations++;
            updateProgress(mIterations, mTotalIterations);
        }

        return result;
    }

    /*
     * it's necessary to first rename all the files to temporary names to avoid conflicts and overwritings
     * when writing the new names.
     */
    private int commitTempFilenames() {
        int result = 0;

        for (MediaItem item : mChangeItems) {
            if (isCancelled()) {
                return result;
            }

            if (item.hasNewFilename()) {
                try {
                    item.commitTempFilename();
                } catch (Exception e) {
                    mMessageConsumer.add("\n" + e);
                    result++;
                }
            }

            mIterations++;
            updateProgress(mIterations, mTotalIterations);
        }

        return result;
    }

    private int commitNewFilenames() {
        int result = 0;

        for (MediaItem item : mChangeItems) {
            if (isCancelled()) {
                return result;
            }

            if (item.hasNewFilename()) {
                try {
                    item.commitNewFilename();
                } catch (Exception e) {
                    mMessageConsumer.add("\n" + e);
                    result++;
                }
            }

            mIterations++;
            updateProgress(mIterations, mTotalIterations);
        }

        return result;
    }
}
