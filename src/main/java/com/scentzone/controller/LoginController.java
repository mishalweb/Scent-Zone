package com.scentzone.controller;

import com.scentzone.service.AuthService;
import com.scentzone.util.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.SQLException;

public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;
    private final AuthService auth = new AuthService();

    @FXML private void login() {
        messageLabel.setText("");
        String email = emailField.getText();
        String password = passwordField.getText();
        if (email.isBlank() || password.isBlank()) {
            messageLabel.setText("Please enter your email and password.");
            return;
        }
        try {
            var user = auth.login(email, password);
            if (user.isPresent()) Navigator.openFor(user.get());
            else messageLabel.setText("Incorrect email or password.");
        } catch (SQLException e) {
            messageLabel.setText("Database unavailable: " + e.getMessage());
        }
    }

    @FXML private void register() { Navigator.register(); }
}
