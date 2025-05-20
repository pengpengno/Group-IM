package com.github.im.group.gui;

import com.github.im.group.gui.controller.DisplayManager;
import com.github.im.group.gui.controller.LoginView;
import com.github.im.group.gui.controller.desktop.chat.ChatMainPresenter;
import com.github.im.group.gui.util.FileIconUtil;
import com.github.im.group.gui.util.ViewUtils;
import com.github.im.group.gui.views.*;
import com.gluonhq.attach.display.DisplayService;
import com.gluonhq.attach.util.Platform;
import com.gluonhq.charm.glisten.afterburner.AppViewRegistry;
import com.gluonhq.charm.glisten.animation.FadeInLeftBigTransition;
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.BottomNavigation;
import com.gluonhq.charm.glisten.control.BottomNavigationButton;
import com.gluonhq.charm.glisten.control.FloatingActionButton;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import com.gluonhq.charm.glisten.visual.Swatch;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import javafx.application.Application;
import javafx.geometry.Dimension2D;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;
import org.scenicview.ScenicView;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ResourceBundle;


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
@SpringBootApplication
public class Main extends Application  {

    private AppManager appManager ;
    private ConfigurableApplicationContext context;


    /**
     * 启动Spring 环境
     */
    public void initSpringEnv() {
        log.info("application init ");
        var springApplication = new SpringApplication(this.getClass());
        springApplication.setWebApplicationType(WebApplicationType.NONE);
//        springApplication.setMainApplicationClass(SpringBootApp.class);
        String mainRunner = System.getProperty("sun.java.command");

        if ("org.springframework.boot.SpringApplicationAotProcessor".equals(mainRunner)) {
            //For Spring's AOT build phase with maven, the SpringContext is sufficient.
            //So we have to stop here because otherwise JavaFX window makes AOT generation fail.
            log.info("Simple run for Spring AOT");
//            return;
        }else{
            springApplication.setHeadless(false); // 启用图形化界面

        }
        context = springApplication.run();



    }


    public void postInit(Scene scene) {

//         桌面端处理
        if (Platform.isDesktop()) {
            Dimension2D dimension2D = DisplayService.create()
                    .map(DisplayService::getDefaultDimensions)
                    .orElse(new Dimension2D(800, 600));
            scene.getWindow().setWidth(dimension2D.getWidth());
            scene.getWindow().setHeight(dimension2D.getHeight());
        }

//        ScenicView.show(scene);

    }

    public static final String OTHER_VIEW = "HOME_VIEW";
    @Override
    public void init() throws Exception {
        appManager = AppManager.initialize(this::postInit);

        initSpringEnv();

        AppViewManager.createHomeView(LoginPresenter.class);
        AppViewManager.createView(MainPresenter.class);
        AppViewManager.registerViewsAndDrawer();




    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        primaryStage.initStyle(StageStyle.UNDECORATED);


        log.info("start application ");
        appManager.start(primaryStage);

//        CSSFX.start();
        UserAgentBuilder.builder()
                .themes(JavaFXThemes.MODENA)
                .themes(MaterialFXStylesheets.forAssemble(true))
                .setDeploy(true)
                .setResolveAssets(true)
                .build()
                .setGlobal();

//
        FileIconUtil.setStageIcon(primaryStage);
        initDrag(primaryStage);
//
        primaryStage.setFullScreen(false);
//        primaryStage.setWidth(800);
//        primaryStage.setHeight(600);
        primaryStage.centerOnScreen();
        appManager.switchView(Main.OTHER_VIEW);

    }

    private static class Delta {
        double x, y;
    }



    private void initDrag(Stage primaryStage) {
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
    }

    public static void main(String[] args) {
        launch(args);
    }
}