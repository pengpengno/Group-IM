package com.github.im.group.gui.controller.mobile;

import com.github.im.common.connect.enums.PlatformType;
import com.github.im.dto.user.LoginRequest;
import com.github.im.group.gui.api.UserEndpoint;
import com.github.im.group.gui.controller.LoginView;
import com.github.im.group.gui.lifecycle.LoginLifecycle;
import com.gluonhq.charm.glisten.control.Alert;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@Slf4j
//@FxView(fxmlName = "login_view")
@RequiredArgsConstructor
public class MobileLoginView implements LoginView {
    @Override
    public PlatformType getPlatform() {
        return PlatformType.MOBILE;
    }

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    private final UserEndpoint userEndpoint;

    private final LoginLifecycle loginLifecycle;


    @FXML
    public void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

//        AppViewRegistry.getInstance().getView("chat").ifPresent(view -> {
//            view.fireEvent(new AppViewRegistry.NavigateEvent("chat"));
//        });

        LoginRequest loginRequest = new LoginRequest(username, password);

        var userInfo = userEndpoint.loginUser(loginRequest)
                .doOnError(throwable -> {
                    // Handle login failure
                    if (throwable instanceof WebClientResponseException exception) {
                        Platform.runLater(() -> {
                            showAlert("Login failed: " + exception.getMessage());
                        });
                    } else {
                        Platform.runLater(() -> {
                            showAlert("An unexpected error occurred.");
                        });
                    }
                    log.error("Login attempt failed", throwable);
                }).block()
                ;

        loginLifecycle.loginCallBack(userInfo);

//        // 模拟简单的用户验证
//        if ("admin".equals(username) && "admin123".equals(password)) {
//            showAlert("Login Successful", "Welcome, " + username + "!");
//        } else {
//            showAlert("Login Failed", "Invalid credentials. Please try again.");
//        }
    }


    // Handle forgot password link click
    public void handleForgotPassword() {
        // Navigate to the password recovery page or show an alert
        showAlert("Forgot Password", "Password recovery is not implemented yet.");
    }
    private void showAlert( String message) {
        showAlert("error",message);
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
