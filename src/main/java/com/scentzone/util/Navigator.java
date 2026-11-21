package com.scentzone.util;

import com.scentzone.controller.AdminController;
import com.scentzone.controller.CustomerController;
import com.scentzone.model.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public final class Navigator {
    private static Stage primaryStage;
    private Navigator() {}

    public static void initialize(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("Scent Zone | Luxury Fragrance Studio");
        primaryStage.setMinWidth(980);
        primaryStage.setMinHeight(660);
    }

    public static void login() { show("login.fxml", 1100, 720, null); }
    public static void register() { show("register.fxml", 1100, 720, null); }
    public static void openFor(User user) {
        if (user.isAdmin()) show("admin.fxml", 1320, 820, user);
        else show("customer.fxml", 1320, 820, user);
    }

    private static void show(String file, int width, int height, User user) {
        try {
            FXMLLoader loader = new FXMLLoader(Navigator.class.getResource("/fxml/" + file));
            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);
            scene.getStylesheets().add(Navigator.class.getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();
            primaryStage.show();
            if (user != null) {
                Object controller = loader.getController();
                if (controller instanceof AdminController admin) admin.setUser(user);
                if (controller instanceof CustomerController customer) customer.setUser(user);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not load " + file + ": " + e.getMessage(), e);
        }
    }
}
