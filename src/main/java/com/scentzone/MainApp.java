package com.scentzone;

import com.scentzone.util.Navigator;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override public void start(Stage stage) {
        Navigator.initialize(stage);
        Navigator.login();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
