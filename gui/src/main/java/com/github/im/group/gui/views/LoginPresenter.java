package com.github.im.group.gui.views;

import com.github.im.dto.user.LoginRequest;
import com.github.im.group.gui.api.UserEndpoint;
import com.github.im.group.gui.config.SecureSettings;
import com.github.im.group.gui.controller.DisplayManager;
import com.github.im.group.gui.controller.desktop.chat.ChatMainPresenter;
import com.github.im.group.gui.lifecycle.LoginLifecycle;
import com.github.im.group.gui.util.AvatarGenerator;
import com.github.im.group.gui.util.FxView;
import com.github.im.group.gui.util.PlatformUtils;
import com.github.im.group.gui.util.ViewUtils;
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.control.*;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import  com.gluonhq.charm.glisten.control.ProgressIndicator;
import java.util.ArrayList;
import java.util.Collections;

@Service
@Slf4j
@RequiredArgsConstructor
@FxView(fxmlName = "login_view",title = "登录")
//public class LoginPresenter implements  LoginView {
public class LoginPresenter  implements  PlatformUI{
//public class LoginPresenter extends View implements  LoginView {



    @FXML
    private View loginView ;

    @FXML
    private TextField usernameField;

    @FXML
    public Avatar logoImageView;


    @FXML
    private GridPane loginGridPane;

    @FXML
    private PasswordField passwordField;

    @FXML
    private MFXButton loginButton;
//    @FXML  private MFXButton navigateToRegister;

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private Label errorLabel;

    private final UserEndpoint userEndpoint;
    private final ProxyPresenter proxyPresenter;

    private final LoginLifecycle loginLifecycle;
    private final ChatMainPresenter chatMainPresenter;


    @Override
    public void desktop() {

    }

    @Override
    public void mobile() {
        DisplayManager.getPrimaryStage().setFullScreen(true);


    }

    /**
     * Gluon 会自动跳动这个方案
     */
    public void initialize() {


        // Attach the login action to the login button
        // 1. 本地检索 上次的数据 登录
        if (autoLogin()){
            // 如果本地存在凭据 ，那么就修改为自动登录的界面

            // 如果是 桌面端 那么就 先展示 登录界面， 移动端就 直接登录 然后跳转界面就行
            if(PlatformUtils.isDesktop()){
                autoLoginUi();
            }else{
                // 直接登录
                login();
            }
        }
        usernameField.setText("kl");
        passwordField.setText("1");

        var view = AppViewManager.createView(proxyPresenter);
        view.registerView();
        ViewUtils.buildDrawer(view);


        loginView.showingProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {

                AppBar appBar = AppManager.getInstance().getAppBar();
                appBar.getActionItems().addAll(
                    MaterialDesignIcon.SETTINGS.button(e -> {
                        AppManager.getInstance().switchView(ProxyPresenter.class.getSimpleName());
                    }));

            }
        });

        var chatMainPresenterView = AppViewManager.createView(chatMainPresenter);
        chatMainPresenterView.registerView();
        ViewUtils.buildDrawer(chatMainPresenterView);

    }



    /**
     * 切换到桌面段自动登录的ul
     */
    private void autoLoginUi() {
        // 自动登录界面 圆角头像    下方蓝底白字的登录按钮 别的组件都不展示去除
        loginView.setPrefSize(200,500);

        loginButton.setPrefHeight(50);
//        AvatarPane avtarPanel = new AvtarPanel();
        var name = SecureSettings.getUserName().get();
        Image img  = AvatarGenerator.generateSquareAvatarWithRoundedCorners(name, AvatarGenerator.AvatarSize.LARGE.getSize());
        logoImageView.setImage(img);
        logoImageView.setPrefSize(70,70);

        logoImageView.setVisible(true);

        var strings = FXCollections.observableList(Collections.singletonList(name));

        AvatarPane<String> avatarPane = new AvatarPane<>(strings);
        avatarPane.setAvatarFactory(speaker -> {
            Avatar avatar = new Avatar();
            avatar.setImage(img);
            return avatar;
        });
        avatarPane.setContentFactory(speaker -> {
            VBox container = new VBox();
            Label nameLabel = new Label(name);
            nameLabel.setWrapText(true);
//            Label jobTitle = new Label(speaker.getJobTitle());
//            jobTitle.setWrapText(true);
//            Label company = new Label(speaker.getCompany());
//            company.setWrapText(true);
//            Label summary = new Label(speaker.getSummary());
//            summary.setWrapText(true);
            container.getChildren().addAll(nameLabel);
            return container;
        });
        loginView.getChildren().add(avatarPane);
        loginGridPane.setVisible(false);


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
//                Services.get(Notification).ifPresent(service -> {
//                    service.showNotification(title, message);
//                });
//                NotificationUtil.showNotification("新消息", "你有一条新消息来自小明");

                SecureSettings.clearTokens();
            }
        });

        new Thread(loginTask).start();
    }

    private void displayError(String message) {
        errorLabel.setText(message);
        log.error(message);
    }
}
