package com.github.im.group.gui.controller.mobile;

import com.github.im.group.gui.controller.LoginView;
import com.gluonhq.charm.glisten.control.Alert;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.stereotype.Component;

@Component
public class MobileLoginView implements LoginView {
    @Override
    public PlatformType getPlatform() {
        return PlatformType.ANDROID;
    }

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    public void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // 模拟简单的用户验证
        if ("admin".equals(username) && "admin123".equals(password)) {
            showAlert("Login Successful", "Welcome, " + username + "!");
        } else {
            showAlert("Login Failed", "Invalid credentials. Please try again.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setContentText(message);
//        alert.setTitle(title);
//        alert.setMessage(message);
//        alert.show();
        alert.showAndWait();
    }
}
