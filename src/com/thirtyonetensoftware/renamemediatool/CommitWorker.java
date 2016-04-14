package com.thirtyonetensoftware.renamemediatool;

import javafx.concurrent.Task;
import javafx.scene.control.TextArea;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;

import java.io.IOException;
import java.util.ArrayList;

public class CommitWorker extends Task<Integer> {

    // ------------------------------------------------------------------------
    // Instance Variables
    // ------------------------------------------------------------------------

    private final ArrayList<MediaItem> mChangeItems;

    private final MessageConsumer mMessageConsumer;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    public CommitWorker(TextArea textArea, ArrayList<MediaItem> changeItems) {
        mChangeItems = changeItems;

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

//        result += commitNewFilenames();

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
                    mMessageConsumer.add(e.getMessage());
                    result++;
                }
            }
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
                    mMessageConsumer.add(e.getMessage());
                    result++;
                }
            }
        }

        return result;
    }
}
