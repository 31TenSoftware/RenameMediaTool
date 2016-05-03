package com.thirtyonetensoftware.renamemediatool;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;

public class Main extends Application {

    // ------------------------------------------------------------------------
    // Super Methods
    // ------------------------------------------------------------------------

    @Override
    public void start(Stage primaryStage) throws Exception {
        // which layout to load, path is relative to this class
        FXMLLoader loader = new FXMLLoader(getClass().getResource("javafx" + File.separator + "layout.fxml"));

        Parent root = loader.load();

        primaryStage.setTitle("RenameMediaTool");
        primaryStage.setScene(new Scene(root, 700, 800));
        primaryStage.show();

        // set stage in controller, for the file picker
        Controller controller = loader.getController();
        controller.setStage(primaryStage);
    }

    // ------------------------------------------------------------------------
    // Entry Point
    // ------------------------------------------------------------------------

    public static void main(String[] args) {
        launch(args);
    }
}
