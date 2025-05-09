package com.github.im.group.gui.controller.desktop;

import com.github.im.common.connect.enums.PlatformType;
import com.github.im.dto.user.LoginRequest;
import com.github.im.group.gui.api.UserEndpoint;
import com.github.im.group.gui.config.SecureSettings;
import com.github.im.group.gui.controller.DisplayManager;
import com.github.im.group.gui.controller.LoginView;
import com.github.im.group.gui.lifecycle.LoginLifecycle;
import com.github.im.group.gui.util.AvatarGenerator;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

@Service
@Slf4j
@RequiredArgsConstructor
public class DesktopLoginView extends View implements Initializable, LoginView {



    @Override
    public PlatformType  getPlatform() {
        return PlatformType.DESKTOP;
    }

    @FXML
    private TextField usernameField;

    @FXML  public ImageView logoImageView;


    @FXML
    private GridPane loginGridPane;

    @FXML
    private PasswordField passwordField;

    @FXML
    private MFXButton loginButton;
    @FXML  private MFXButton navigateToRegister;

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private Label errorLabel;

    private final UserEndpoint userEndpoint;

    private final LoginLifecycle loginLifecycle;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Attach the login action to the login button
        // 1. 本地检索 上次的数据 登录
        if (autoLogin()){
            // 如果本地存在凭据 ，那么就修改为自动登录的界面

            // 如果是 桌面端 那么就 先展示 登录界面， 移动端就 直接登录 然后跳转界面就行
            if(getPlatform() == PlatformType.DESKTOP){
                autoLoginUi();
            }else{
                // 直接登录
                login();
            }
        }
        usernameField.setText("kl");
        passwordField.setText("1");

    }

    @FXML
    private void navigateToRegister() {
//        TODO
    }

    /**
     * 切换到桌面段自动登录的ul
     */
    private void autoLoginUi() {
        // 自动登录界面 圆角头像    下方蓝底白字的登录按钮 别的组件都不展示去除
        var primaryStage = DisplayManager.getPrimaryStage();
        primaryStage.setWidth(300);
        primaryStage.setHeight(500);
        primaryStage.centerOnScreen();

        loginButton.setPrefHeight(50);

        loginGridPane.setVisible(false);
        var image = AvatarGenerator.generateSquareAvatarWithRoundedCorners(SecureSettings.getUserName().get(), 50);

        logoImageView.setImage(image);
        logoImageView.setVisible(true);
        navigateToRegister.setVisible(false);


    }

    public boolean autoLogin(){
        return  SecureSettings.getSecretToken().isPresent();
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

        LoginRequest loginRequest = new LoginRequest(username,
                password,
                SecureSettings.getSecretToken().orElse(null));

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

        loginTask.setOnSucceeded(event -> {
            // Re-enable the login button and hide progress indicator
            loginButton.setDisable(false);
            progressIndicator.setVisible(false);
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
