package com.scentzone.controller;

import com.scentzone.service.AuthService;
import com.scentzone.util.Dialogs;
import com.scentzone.util.Navigator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.sql.SQLException;

public class RegisterController {
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private Label messageLabel;
    private final AuthService auth = new AuthService();

    @FXML private void register() {
        messageLabel.setText("");
        try {
            auth.register(nameField.getText(), emailField.getText(),
                    passwordField.getText(), confirmField.getText());
            Dialogs.info("Account created", "You can now sign in with your customer account.");
            Navigator.login();
        } catch (IllegalArgumentException e) {
            messageLabel.setText(e.getMessage());
        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) messageLabel.setText("This email is already registered.");
            else messageLabel.setText("Database error: " + e.getMessage());
        }
    }
    @FXML private void back() { Navigator.login(); }
}
