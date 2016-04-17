package com.thirtyonetensoftware.renamemediatool;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;

public class Controller {

    // ------------------------------------------------------------------------
    // Instance Variables
    // ------------------------------------------------------------------------

    @FXML
    private Label mPathLabel;

    @FXML
    private Label mChangesLogLabel;

    @FXML
    private ProgressBar mProgressBar;

    @FXML
    private TextArea mOutputBox;

    @FXML
    private Button mWriteChangesButton;

    private Stage mStage;

    private final DirectoryChooser mDirectoryChooser = new DirectoryChooser();

    private ProcessWorker mTask;

    private final ArrayList<MediaItem> mChangeItems = new ArrayList<>();

    // ------------------------------------------------------------------------
    // Layout Methods
    // ------------------------------------------------------------------------

    public void onChooseDirectoryButtonClick() {
        File file = mDirectoryChooser.showDialog(mStage);

        if (file != null) {
            mPathLabel.setText(file.getPath());
            mChangesLogLabel.setText(file.getPath() + File.separator + "changes.csv");

            mTask = new ProcessWorker(this, mOutputBox, file, mChangeItems);

            mProgressBar.progressProperty().unbind();
            mProgressBar.setProgress(0);
            mProgressBar.progressProperty().bind(mTask.progressProperty());

            Thread mThread = new Thread(mTask);
            mThread.setDaemon(true);
            mThread.start();
        }
    }

    public void onStopButtonClick() {
        mProgressBar.progressProperty().unbind();
        mProgressBar.setProgress(0);

        if (mTask != null) {
            mTask.cancel();
        }
    }

    public void enableWriteButton(boolean enabled) {
        mWriteChangesButton.setDisable(!enabled);
    }

    public void onWriteChangesButtonClick() {
        CommitWorker committer = new CommitWorker(mOutputBox, mChangeItems);

        mOutputBox.appendText("\n\nCOMMITTING CHANGES... DO NOT CLOSE PROGRAM!");

        mProgressBar.progressProperty().unbind();
        mProgressBar.setProgress(0);
        mProgressBar.progressProperty().bind(committer.progressProperty());

        Thread mThread = new Thread(committer);
        mThread.setDaemon(true);
        mThread.start();
    }

    // ------------------------------------------------------------------------
    // Instance Methods
    // ------------------------------------------------------------------------

    public void setStage(Stage stage) {
        mStage = stage;
    }
}
