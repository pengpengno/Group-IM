package com.github.im.group.gui;

import com.github.im.group.gui.controller.LoginView;
import com.github.im.group.gui.controller.MainController;
import com.github.im.group.gui.util.FxmlLoader;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import reactor.core.publisher.Mono;

import java.io.IOException;

/**
 * Description:
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2024/10/17
 */
@SpringBootApplication
public class Main extends Application {

    private Stage primaryStage;

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("IM Platform");

        // 设置图标
//        this.primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icon.png")));

        initRootLayout();
    }

    @Override
    public void init() {
        SpringApplication app = new SpringApplication(Main.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        applicationContext = app.run();

    }

    @Override
    public void stop() {
        applicationContext.close();
    }

    private void initRootLayout() {

        var scene = FxmlLoader.getSceneInstance(LoginView.class);

        // 加载BootstrapFX样式
        scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}