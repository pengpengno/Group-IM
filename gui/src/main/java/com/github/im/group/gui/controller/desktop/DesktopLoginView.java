package com.github.im.group.gui.controller.desktop;

import com.github.im.common.connect.enums.PlatformType;
import com.github.im.dto.user.LoginRequest;
import com.github.im.group.gui.api.UserEndpoint;
import com.github.im.group.gui.controller.LoginView;
import com.github.im.group.gui.lifecycle.LoginLifecycle;
import com.gluonhq.charm.glisten.mvc.View;
import io.github.palexdev.materialfx.controls.MFXButton;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URL;
import java.util.ResourceBundle;

@Service
@Slf4j
@RequiredArgsConstructor
public class DesktopLoginView extends View implements Initializable, LoginView {
//public class DesktopLoginView  implements Initializable, LoginView {

    @Override
    public PlatformType  getPlatform() {
        return PlatformType.DESKTOP;
    }

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private MFXButton loginButton;

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private Label errorLabel;

    private final UserEndpoint userEndpoint;

    private final LoginLifecycle loginLifecycle;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Attach the login action to the login button
//        loginButton.setOnAction(event -> login());
        usernameField.setText("kl");
        passwordField.setText("1");

    }

    @FXML
    private void navigateToRegister() {


    }




    @PostConstruct
    public void init() {

    }

    @FXML
    private void login() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        log.debug("username {}  , password {} ", username, password);

        if (username.isEmpty() || password.isEmpty()) {
            displayError("Please enter both username and password.");
            return;
        }

        LoginRequest loginRequest = new LoginRequest(username, password);

        // Disable the login button and show progress indicator
        loginButton.setDisable(true);
        progressIndicator.setVisible(true);
        errorLabel.setText("");

        Task<Void> loginTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                var userInfo = userEndpoint.loginUser(loginRequest)
                        .doOnError(throwable -> {
                            // Handle login failure
                            if (throwable instanceof WebClientResponseException exception) {
                                Platform.runLater(() -> {
                                    displayError("Login failed: " + exception.getMessage());
                                });
                            } else {
                                Platform.runLater(() -> {
                                    displayError("An unexpected error occurred.");
                                });
                            }
                            log.error("Login attempt failed", throwable);
                        }).block();

                if (userInfo != null) {
                    loginLifecycle.loginCallBack(userInfo.getBody());
                }
                return null;
            }
        };

        loginTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                // Re-enable the login button and hide progress indicator
                loginButton.setDisable(false);
                progressIndicator.setVisible(false);
            }
        });

        loginTask.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                // Re-enable the login button and hide progress indicator
                loginButton.setDisable(false);
                progressIndicator.setVisible(false);
            }
        });

        new Thread(loginTask).start();
    }

    private void displayError(String message) {
        errorLabel.setText(message);
        log.error(message);
    }
}
