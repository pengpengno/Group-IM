package com.github.im.group.gui;

import com.github.im.group.gui.controller.Display;
import com.github.im.group.gui.controller.LoginView;
import com.github.im.group.gui.controller.desktop.DesktopLoginView;
import com.github.im.group.gui.util.CssLoaderUtil;
import com.github.im.group.gui.util.FxmlLoader;
import com.gluonhq.attach.display.DisplayService;
import com.gluonhq.attach.util.Platform;
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import jakarta.inject.Inject;
import javafx.application.Application;
import javafx.geometry.Dimension2D;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.scenicview.ScenicView;
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
//@SpringBootApplication
@Slf4j
public class Main extends Application {

    @Inject
    private AppManager appManager;

    public void initApp() {
        log.info("application init ");
//        var springApplication = new SpringApplication(Main.class);
        var springApplication = new SpringApplication(SpringBootApp.class);
        springApplication.setWebApplicationType(WebApplicationType.NONE);
        springApplication.setMainApplicationClass(SpringBootApp.class);
        springApplication.run();
        String mainRunner = System.getProperty("sun.java.command");
        if ("org.springframework.boot.SpringApplicationAotProcessor".equals(mainRunner)) {
            //For Spring's AOT build phase with maven, the SpringContext is sufficient.
            //So we have to stop here because otherwise JavaFX window makes AOT generation fail.
            log.info("Simple run for Spring AOT");
            return;
        }

//        System.setProperty("javafx.platform", "DESKTOP");
        // **延迟初始化 AppManager**
        appManager = AppManager.initialize((scene)->postInit(scene));




//        MaterialDesignIcon.MIC_NONE
    }

    public void postInit(Scene scene) {

        Display.display(LoginView.class);

//         桌面端处理
        if (Platform.isDesktop()) {
            Dimension2D dimension2D = DisplayService.create()
                    .map(DisplayService::getDefaultDimensions)
                    .orElse(new Dimension2D(640, 480));
            scene.getWindow().setWidth(dimension2D.getWidth());
            scene.getWindow().setHeight(dimension2D.getHeight());
        }

//        AppViewManager.registerViewsAndDrawer();



    }

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


        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle("IM Platform");


        // 设置图标
        var iconResource = new ClassPathResource("images/icon.png");
        var stylesResource = new ClassPathResource("css/styles.png");
        if (iconResource.exists()){
            primaryStage.getIcons().add(new Image(iconResource.getInputStream()));
        }
        if(stylesResource.exists()){

            var scene = primaryStage.getScene();
            CssLoaderUtil.loadCss(scene,"css/styles.css");
            CssLoaderUtil.loadCss(scene,"css/chat.css");
//            scene.getStylesheets().add(stylesResource.getURL().toExternalForm());
        }

        primaryStage.setResizable(true);

        initApp();


        appManager.start(primaryStage);


        Display.setPrimaryStage(primaryStage);

        final double[] offsetX = {0}, offsetY = {0};
        var appBar = AppManager.getInstance().getAppBar();

        appBar.setOnMousePressed(event -> {
            offsetX[0] = event.getSceneX();
            offsetY[0] = event.getSceneY();
        });

        appBar.setOnMouseDragged(mouseEvent -> {

            if (!primaryStage.isMaximized()) {
                primaryStage.setX(mouseEvent.getScreenX() - offsetX[0]);
                primaryStage.setY(mouseEvent.getScreenY() - offsetY[0]);
            }
        });


        appBar.setNavIcon(MaterialDesignIcon.MENU.button(e -> System.out.println("nav icon")));
//        ScenicView.show(primaryStage.getScene());

        // **手动取消全屏**
        primaryStage.setFullScreen(false);


    }




    public static void main(String[] args) {
        launch(args);
    }
}