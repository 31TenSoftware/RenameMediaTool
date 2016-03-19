package com.thirtyonetensoftware.renamemediatool;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

public class Controller {

    // ------------------------------------------------------------------------
    // Instance Variables
    // ------------------------------------------------------------------------

    @FXML
    private Label mPath;

    @FXML
    private ProgressBar mProgressBar;

    @FXML
    private TextArea mResults;

    private Stage mStage;

    private final DirectoryChooser mDirectoryChooser = new DirectoryChooser();

    private Worker mTask;

    // ------------------------------------------------------------------------
    // Layout Methods
    // ------------------------------------------------------------------------

    public void onChooseDirectoryButtonClick() {
        File file = mDirectoryChooser.showDialog(mStage);

        if (file != null) {
            mPath.setText(file.getPath());

            mTask = new Worker(mResults, file);
            mProgressBar.setProgress(0);
            mProgressBar.progressProperty().bind(mTask.progressProperty());

            Thread mThread = new Thread(mTask);
            mThread.setDaemon(true);
            mThread.start();
        }
    }

    public void onStopButtonClick() {
        if (mTask != null) {
            mProgressBar.setProgress(0);
            mProgressBar.progressProperty().unbind();
            mTask.cancel();
        }
    }

    // ------------------------------------------------------------------------
    // Instance Methods
    // ------------------------------------------------------------------------

    public void setStage(Stage stage) {
        mStage = stage;
    }
}
