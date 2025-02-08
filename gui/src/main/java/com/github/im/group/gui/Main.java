package com.github.im.group.gui;

import com.github.im.group.gui.controller.LoginView;
import com.github.im.group.gui.util.FxmlLoader;
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.mvc.View;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
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
//@SpringBootApplication
public class Main extends Application {

//    private static Stage primaryStage;
    private ConfigurableApplicationContext applicationContext;
    private AppManager appManager;

    @Override
    public void init() {
        // **手动启动 SpringBoot**
//        applicationContext = new SpringApplicationBuilder(SpringBootApp.class)
//                .web(WebApplicationType.NONE) // 关闭 Web 环境
//                .run();


        var springApplication = new SpringApplication(SpringBootApp.class);
        springApplication.setWebApplicationType(WebApplicationType.NONE);
        applicationContext = springApplication.run();


        // **延迟初始化 AppManager**
        appManager = AppManager.initialize((scene)->postInit());

//        appManager.addViewFactory(AppManager.HOME_VIEW,
//                () -> new View(FxmlLoader.getSceneInstance(LoginView.class).getRoot()));


    }

    public void postInit() {

        appManager.addViewFactory("LOGIN_VIEW",
                () -> new View(FxmlLoader.getSceneInstance(LoginView.class).getRoot()));

        appManager.switchView("LOGIN_VIEW");
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
//        this.primaryStage = primaryStage;
//        StageManager.setPrimaryStage(primaryStage);

        UserAgentBuilder.builder()
                .themes(JavaFXThemes.MODENA)
                .themes(MaterialFXStylesheets.forAssemble(true))
                .setDeploy(true)
                .setResolveAssets(true)
                .build()
                .setGlobal();

        primaryStage.setTitle("IM Platform");


        // 设置图标
        var iconResource = new ClassPathResource("images/icon.png");
        if (iconResource.exists()){
            primaryStage.getIcons().add(new Image(iconResource.getInputStream()));

        }
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.setWidth(1000);  // 设置默认宽度
        primaryStage.setHeight(700);  // 设置默认高度
        primaryStage.setResizable(true);

        appManager.start(primaryStage);
        // **手动取消全屏**
        primaryStage.setFullScreen(false);
    }

    @Override
    public void stop() {
        applicationContext.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}