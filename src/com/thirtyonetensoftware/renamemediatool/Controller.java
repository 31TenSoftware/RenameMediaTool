package com.thirtyonetensoftware.renamemediatool;

import javafx.fxml.FXML;
import javafx.scene.control.*;
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
    private CheckBox mStaggerDateTimes;

    @FXML
    private ProgressBar mProgressBar;

    @FXML
    private TextArea mOutputBox;

    @FXML
    private Button mWriteChangesButton;

    private Stage mStage;

    private File mRootDir;

    private final DirectoryChooser mDirectoryChooser = new DirectoryChooser();

    private ProcessWorker mTask;

    private ArrayList<MediaItem> mChangeItems;

    // ------------------------------------------------------------------------
    // Layout Methods
    // ------------------------------------------------------------------------

    public void onChooseDirectoryButtonClick() {
        mDirectoryChooser.setInitialDirectory(mRootDir != null && mRootDir.exists() ?
                mRootDir : new File(System.getProperty("user.dir")));
        mRootDir = mDirectoryChooser.showDialog(mStage);

        if (mRootDir != null) {
            mPathLabel.setText(mRootDir.getPath());

            mChangeItems = new ArrayList<>();

            mTask = new ProcessWorker(this, mOutputBox, mRootDir, mStaggerDateTimes.isSelected(), mChangeItems);

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
