package com.github.im.group.gui.controller;

import com.github.im.dto.LoginRequest;
import com.github.im.group.gui.api.UserEndpoint;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@Slf4j
//@RequiredArgsConstructor
public class LoginView extends VBox {
    
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @Autowired
    private  UserEndpoint userEndpoint;

    public LoginView() {
        // 初始化 WebClient
        initialize();
    }

    private void initialize() {
        loginButton.setOnAction(event -> login());
    }

    private void login() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        LoginRequest loginRequest = new LoginRequest(username, password);
        
        userEndpoint.loginUser(loginRequest)
            .subscribe(userInfo -> {
                // 登录成功，跳转到主界面
                Platform.runLater(() -> {
                    MainController mainController = new MainController();
                    mainController.show();
                    // 隐藏登录界面或关闭当前窗口
                    this.getScene().getWindow().hide();
                });
            }, throwable -> {
                if (throwable instanceof WebClientResponseException) {
                    // 处理登录失败的情况
                    Platform.runLater(() -> {
                        // 显示错误提示，例如：弹出提示框
                        System.out.println("登录失败: " + throwable.getMessage());
                    });
                }
            });
    }
}
