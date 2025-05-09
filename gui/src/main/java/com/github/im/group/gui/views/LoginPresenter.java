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
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.FloatingActionButton;
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
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
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
@FxView(fxmlName = "login_view")
public class LoginPresenter extends View implements Initializable, LoginView {

    @FXML private View loginView ;
    private static final String OTHER_VIEW = "other";


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

        this.showingProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                AppBar appBar = AppManager.getInstance().getAppBar();
                appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                        AppManager.getInstance().getDrawer().open()));
                appBar.setTitleText("主页");
//                appBar.setTitleText(resources.getString("appbar.title"));
//                appBar.getActionItems().add(filterButton);
            }
        });


        final var appManager = AppManager.getInstance();
        appManager.viewProperty().addListener((obs, ov, nv) -> {
            AppBar appBar = AppManager.getInstance().getAppBar();
//            AppBar appBar = AppManager.getInstance().getAppBar();
            appBar.setNavIcon(MaterialDesignIcon.MENU.button(e ->
                    AppManager.getInstance().getDrawer().open()));
            appBar.setTitleText("主页");
            var id = nv.getId();
            if(!StringUtils.hasLength(id)){
                return;
            }
            switch(id) {
                case HOME_VIEW:
                    appBar.setNavIcon(MaterialDesignIcon.HOME.button(e -> appManager.switchView(HOME_VIEW)));
                    appBar.setTitleText("Home View");
                    Swatch.TEAL.assignTo(appBar.getScene());
                    break;
                case OTHER_VIEW:
                    appBar.setNavIcon(MaterialDesignIcon.HTTPS.button(e -> appManager.switchView(OTHER_VIEW)));
                    appBar.setTitleText("Other View");
                    appBar.setVisible(true);
                    break;
            }
        });
        appManager.addViewFactory(OTHER_VIEW, () -> new View(new CheckBox("I like Glisten")));
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
//        var primaryStage = DisplayManager.getPrimaryStage();
//        primaryStage.setWidth(300);
//        primaryStage.setHeight(500);
//        primaryStage.centerOnScreen();
//        loginView.setPrefSize(300,500);

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
