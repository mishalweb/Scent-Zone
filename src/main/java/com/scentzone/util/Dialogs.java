package com.scentzone.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;

public final class Dialogs {
    private Dialogs() {}

    public static void info(String heading, String message) { show(Alert.AlertType.INFORMATION, heading, message); }
    public static void error(String heading, String message) { show(Alert.AlertType.ERROR, heading, message); }
    private static void show(Alert.AlertType type, String heading, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle("Scent Zone");
        alert.setHeaderText(heading);
        alert.showAndWait();
    }
    public static boolean confirm(String heading, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        alert.setTitle("Scent Zone");
        alert.setHeaderText(heading);
        Optional<ButtonType> answer = alert.showAndWait();
        return answer.isPresent() && answer.get() == ButtonType.OK;
    }
}
