package com.github.im.group.gui;

import com.github.im.group.gui.controller.DisplayManager;
import com.github.im.group.gui.controller.LoginView;
import com.github.im.group.gui.util.CssLoaderUtil;
import com.gluonhq.attach.display.DisplayService;
import com.gluonhq.attach.util.Platform;
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.control.AppBar;
import com.google.inject.Inject;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import javafx.application.Application;
import javafx.geometry.Dimension2D;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
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
@Slf4j
public class Main extends Application {
//
//    @Inject
    private AppManager appManager;

    /**
     * 启动Spring 环境
     */
    public void initSpringEnv() {
        log.info("application init ");
        var springApplication = new SpringApplication(SpringBootApp.class);
        springApplication.setWebApplicationType(WebApplicationType.NONE);
        springApplication.setMainApplicationClass(SpringBootApp.class);
        springApplication.setHeadless(false); // 启用图形化界面
        springApplication.run();
        String mainRunner = System.getProperty("sun.java.command");
        if ("org.springframework.boot.SpringApplicationAotProcessor".equals(mainRunner)) {
            //For Spring's AOT build phase with maven, the SpringContext is sufficient.
            //So we have to stop here because otherwise JavaFX window makes AOT generation fail.
            log.info("Simple run for Spring AOT");
            return;
        }
//

    }

    public void postInit(Scene scene) {

        DisplayManager.display(LoginView.class);

////         桌面端处理
//        if (Platform.isDesktop()) {
//            Dimension2D dimension2D = DisplayService.create()
//                    .map(DisplayService::getDefaultDimensions)
//                    .orElse(new Dimension2D(640, 480));
//            scene.getWindow().setWidth(dimension2D.getWidth());
//            scene.getWindow().setHeight(dimension2D.getHeight());
//        }

    }

    @Override
    public void start(Stage primaryStage) throws IOException {

        log.info("start application ");

//        CSSFX.start();
        DisplayManager.setPrimaryStage(primaryStage);


        UserAgentBuilder.builder()
                .themes(JavaFXThemes.MODENA)
                .themes(MaterialFXStylesheets.forAssemble(true))
                .setDeploy(true)
                .setResolveAssets(true)
                .build()
                .setGlobal();


        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle("IM Platform");

        primaryStage.setResizable(true);
        var iconResource = new ClassPathResource("images/icon.png");

        if (iconResource.exists()){
            try {
                primaryStage.getIcons().add(new Image(iconResource.getInputStream()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        initSpringEnv();

        // **延迟初始化 AppManager**
        appManager = AppManager.initialize((scene)->postInit(scene));
        appManager.start(primaryStage);

        var appBar = appManager.getAppBar();
        final Delta dragDelta = new Delta();

        appBar.setOnMousePressed(event -> {
            if (event.isPrimaryButtonDown()) {
                dragDelta.x = primaryStage.getX() - event.getScreenX();
                dragDelta.y = primaryStage.getY() - event.getScreenY();
            }
        });

        appBar.setOnMouseDragged(event -> {
            if (event.isPrimaryButtonDown()) {
                primaryStage.setX(event.getScreenX() + dragDelta.x);
                primaryStage.setY(event.getScreenY() + dragDelta.y);
            }
        });
        primaryStage.setFullScreen(false);
        primaryStage.setWidth(800);
        primaryStage.setHeight(600);
        primaryStage.centerOnScreen();

    }

    private static class Delta {
        double x, y;
    }



    public static void main(String[] args) {
        launch(args);
    }
}