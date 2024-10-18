package com.github.im.group.gui.controller;

import com.github.im.dto.user.LoginRequest;
import com.github.im.group.gui.api.UserEndpoint;
import com.github.im.group.gui.util.FxView;
import com.github.im.group.gui.util.FxmlLoader;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URL;
import java.util.ResourceBundle;

@Service
@Slf4j
@FxView(path = "login_view")
public class LoginView extends VBox  implements Initializable {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;
    @FXML
    private Button navigateToRegister;

    @FXML
    private VBox rootPane;

    @Autowired
    private UserEndpoint userEndpoint;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Attach the login action to the login button
        loginButton.setOnAction(event -> login());
    }

    @FXML
    private void navigateToRegister() {

        var stage = FxmlLoader.applySingleStage(RegisterView.class);
        this.getScene().getWindow().hide();
        stage.show();

    }
    @FXML
    private void login() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            displayError("Please enter both username and password.");
            return;
        }

        LoginRequest loginRequest = new LoginRequest(username, password);

        userEndpoint.loginUser(loginRequest)
                .subscribe(userInfo -> {
                    // On successful login, navigate to the main view
                    Platform.runLater(() -> {
                        var stage = FxmlLoader.applySingleStage(MainController.class);
                        this.getScene().getWindow().hide();
                        stage.show();
                    });
                }, throwable -> {
                    // Handle login failure
                    if (throwable instanceof WebClientResponseException) {
                        WebClientResponseException exception = (WebClientResponseException) throwable;
                        Platform.runLater(() -> {
                            displayError("Login failed: " + exception.getMessage());
                        });
                    } else {
                        Platform.runLater(() -> {
                            displayError("An unexpected error occurred.");
                        });
                    }
                    log.error("Login attempt failed", throwable);
                });
    }

    private void displayError(String message) {
        // Display error messages (replace this with your preferred error display logic)
        log.error(message);
        // Optionally, you could add a Label for errors and set its text here
    }
}
