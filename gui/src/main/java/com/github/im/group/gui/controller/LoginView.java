package com.github.im.group.gui.controller;

import com.github.im.dto.user.LoginRequest;
import com.github.im.group.gui.Main;
import com.github.im.group.gui.api.UserEndpoint;
import com.github.im.group.gui.util.FxView;
import com.github.im.group.gui.util.FxmlLoader;
import com.github.im.group.gui.util.StageManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URL;
import java.util.ResourceBundle;

@Service
@Slf4j
@FxView(path = "login_view")
@RequiredArgsConstructor
public class LoginView extends StackPane implements Initializable {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button navigateToRegister;

    private final UserEndpoint userEndpoint;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Attach the login action to the login button
//        loginButton.setOnAction(event -> login());
    }

    @FXML
    private void navigateToRegister() {

        var scene = FxmlLoader.getSceneInstance(RegisterView.class);
        var primaryStage = StageManager.getPrimaryStage();
        primaryStage.sizeToScene(); // 自动调整主 Stage 大小以适应当前 Scene 的大小

//        如果希望窗口在首次加载时能自适应，你也可以绑定 RegisterView 的宽度和高度到 Scene
//        scene.widthProperty().addListener((observable, oldValue, newValue) -> {
//            primaryStage.setWidth(newValue.doubleValue());
//        });
//        scene.heightProperty().addListener((observable, oldValue, newValue) -> {
//            primaryStage.setHeight(newValue.doubleValue());
//        });

        primaryStage.setResizable(true);
        primaryStage.setScene(scene);


    }
    @FXML
    private void login() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        log.debug("username {}  , password {} ",username,password);

        if (username.isEmpty() || password.isEmpty()) {
            displayError("Please enter both username and password.");
            return;
        }

        LoginRequest loginRequest = new LoginRequest(username, password);

        userEndpoint.loginUser(loginRequest)
                .subscribe(userInfo -> {
                    // On successful login, navigate to the main view
                    Platform.runLater(() -> {
//                        var stage = FxmlLoader.applySingleStage(MainController.class);
                        log.debug("login success {}" ,userInfo);
                        Display.display(MainController.class);
//                        this.getScene().getWindow().hide();
//                        stage.show();
                    });
                }, throwable -> {
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
                });
    }


    private void displayError(String message) {
        // Display error messages (replace this with your preferred error display logic)
        log.error(message);
        // Optionally, you could add a Label for errors and set its text here
    }
}
