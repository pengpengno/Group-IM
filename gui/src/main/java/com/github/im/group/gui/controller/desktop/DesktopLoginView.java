package com.github.im.group.gui.controller.desktop;

import com.github.im.common.connect.enums.PlatformType;
import com.github.im.dto.user.LoginRequest;
import com.github.im.group.gui.api.UserEndpoint;
import com.github.im.group.gui.controller.Display;
import com.github.im.group.gui.controller.LoginView;
import com.github.im.group.gui.controller.MainHomeView;
import com.github.im.group.gui.lifecycle.LoginLifecycle;
import com.github.im.group.gui.util.FxView;
import com.github.im.group.gui.util.FxmlLoader;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import io.github.palexdev.materialfx.controls.MFXButton;
import jakarta.annotation.PostConstruct;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URL;
import java.util.ResourceBundle;

@Service
@Slf4j
//@FxView(fxmlName = "login_view")
@RequiredArgsConstructor
public class DesktopLoginView extends View implements Initializable, LoginView {

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
    private MFXButton navigateToRegister;


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


    @Override
    protected void updateAppBar(AppBar appBar) {
        super.updateAppBar(appBar);
    }

    @PostConstruct
    public void init() {

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
                }).block()
                ;

        loginLifecycle.loginCallBack(userInfo);

    }


    private void displayError(String message) {
        // Display error messages (replace this with your preferred error display logic)
        log.error(message);
        // Optionally, you could add a Label for errors and set its text here
    }
}
