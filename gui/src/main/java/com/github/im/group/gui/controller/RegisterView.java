package com.github.im.group.gui.controller;

import com.github.im.dto.user.RegistrationRequest;
import com.github.im.group.gui.api.UserEndpoint;
import com.github.im.group.gui.util.FxView;
import com.github.im.group.gui.util.FxmlLoader;
import com.github.im.group.gui.util.StageManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URL;
import java.util.ResourceBundle;

@Service
@Slf4j
@FxView(path = "register_view")
public class RegisterView extends VBox implements Initializable {

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private TextField phoneNumberField;

    @FXML
    private Button registerButton;

    @Autowired
    private UserEndpoint userEndpoint;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        registerButton.setOnAction(event -> register());
    }

    @FXML
    private void register() {
        String fullName = fullNameField.getText();
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String phoneNumber = phoneNumberField.getText();

        RegistrationRequest registrationRequest = new RegistrationRequest(
                fullName, username, email, password, confirmPassword, phoneNumber
        );

        userEndpoint.registerUser(registrationRequest)
            .subscribe(userInfo -> {
                // 注册成功后跳转到登录页面或主界面
                Platform.runLater(() -> {
                    var stage = FxmlLoader.getSceneInstance(LoginView.class);
                    StageManager.getPrimaryStage().setScene(stage);
//                        this.getScene().getWindow().hide();
                });
            }, throwable -> {
                if (throwable instanceof WebClientResponseException) {
                    // 处理注册失败的情况
                    Platform.runLater(() -> {
                        // 显示错误提示，例如：弹出提示框
                        System.out.println("注册失败: " + throwable.getMessage());
                    });
                }
            });
    }
}
