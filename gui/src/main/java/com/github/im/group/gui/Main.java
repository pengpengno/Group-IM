package com.github.im.group.gui;

import com.github.im.group.gui.controller.LoginView;
import com.github.im.group.gui.controller.MainController;
import com.github.im.group.gui.util.FxmlLoader;
import com.github.im.group.gui.util.StageManager;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.bootstrapfx.BootstrapFX;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
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

    private static Stage primaryStage;

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void start(Stage primaryStage) throws IOException {

//        CSSFX.start();

        UserAgentBuilder.builder()
                .themes(JavaFXThemes.MODENA)
                .themes(MaterialFXStylesheets.forAssemble(true))
                .setDeploy(true)
                .setResolveAssets(true)
                .build()
                .setGlobal();

        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("IM Platform");


        // 设置图标
        var iconResource = new ClassPathResource("images/icon.png");
        if (iconResource.exists()){
            this.primaryStage.getIcons().add(new Image(iconResource.getInputStream()));

        }

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


        scene.setFill(Color.TRANSPARENT);
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        // 加载BootstrapFX样式
//        scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
//        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        primaryStage.show();

        StageManager.setPrimaryStage(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}