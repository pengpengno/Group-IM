package com.github.im.group.gui.views;

import com.github.im.common.connect.enums.PlatformType;
import com.github.im.dto.user.LoginRequest;
import com.github.im.group.gui.api.UserEndpoint;
import com.github.im.group.gui.config.SecureSettings;
import com.github.im.group.gui.controller.DisplayManager;
import com.github.im.group.gui.controller.LoginView;
import com.github.im.group.gui.lifecycle.LoginLifecycle;
import com.github.im.group.gui.util.AvatarGenerator;
import com.github.im.group.gui.util.FxView;
import com.gluonhq.attach.util.Services;
import com.gluonhq.charm.glisten.animation.BounceInLeftTransition;
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.control.*;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import com.gluonhq.charm.glisten.visual.Swatch;
import io.github.palexdev.materialfx.controls.MFXButton;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URL;
import java.util.ResourceBundle;
import static com.gluonhq.charm.glisten.application.AppManager.HOME_VIEW;

@Service
@Slf4j
@RequiredArgsConstructor
@FxView(fxmlName = "login_view",title = "登录")
public class LoginPresenter implements  LoginView {
//public class LoginPresenter extends View implements  LoginView {



    @Override
    public PlatformType  getPlatform() {
        return PlatformType.DESKTOP;
    }


    @FXML
    private View loginView ;

    @FXML
    private TextField usernameField;

    @FXML
    public ImageView logoImageView;


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


    /**
     * Gluon 会自动跳动这个方案
     */
    public void initialize() {


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


//        loginView.setShowTransitionFactory(BounceInLeftTransition::new);
//
//        loginView.showingProperty().addListener((obs, oldValue, newValue) -> {
//            if (newValue) {
//                var appBar = loginView.getAppManager().getAppBar();
//
////                appBar = AppManager.getInstance().getAppBar();
//
//                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e -> System.out.println("nav icon")));
//
//                appBar.setTitleText("The AppBar");
//
//                appBar.getActionItems().addAll(
//                        MaterialDesignIcon.SEARCH.button(e -> System.out.println("search")),
//                        MaterialDesignIcon.FAVORITE.button(e -> System.out.println("fav")));
//
//                appBar.getMenuItems().addAll(new MenuItem("Settings"));
//
//                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
//                        AppManager.getInstance().getDrawer().open()));
//                appBar.setTitleText("主页");
//            }
//        });
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
        loginView.setPrefSize(300,500);

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
