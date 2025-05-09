package com.github.im.group.gui;

import com.github.im.group.gui.controller.DisplayManager;
import com.github.im.group.gui.util.FileIconUtil;
import com.github.im.group.gui.views.*;
import com.gluonhq.charm.glisten.afterburner.AppViewRegistry;
import com.gluonhq.charm.glisten.animation.FadeInLeftBigTransition;
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.application.MobileApplication;
import com.gluonhq.charm.glisten.control.AppBar;
import com.gluonhq.charm.glisten.control.FloatingActionButton;
import com.gluonhq.charm.glisten.mvc.View;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import com.gluonhq.charm.glisten.visual.Swatch;
import io.github.palexdev.materialfx.theming.JavaFXThemes;
import io.github.palexdev.materialfx.theming.MaterialFXStylesheets;
import io.github.palexdev.materialfx.theming.UserAgentBuilder;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;

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
public class Main extends Application {
//
//    @Inject
    private final  AppManager appManager = AppManager.initialize();

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


    }


    private static final String OTHER_VIEW = "other";
    private static final ResourceBundle bundle = ResourceBundle.getBundle("i18n.drawer");

    @Override
    public void init() throws Exception {

        initSpringEnv();


        AppViewManager.createHomeView(LoginPresenter.class);
        AppViewManager.createView(MainPresenter.class);
        AppViewManager.registerViewsAndDrawer();


//        appManager.viewProperty().addListener((obs, ov, nv) -> {
//            AppBar appBar = AppManager.getInstance().getAppBar();
//            var id = nv.getId();
//            if(!StringUtils.hasLength(id)){
//                return;
//            }
//            switch(id) {
//                case HOME_VIEW:
//                    appBar.setNavIcon(MaterialDesignIcon.HOME.button(e -> appManager.switchView(HOME_VIEW)));
//                    appBar.setTitleText("Home View");
//                    Swatch.TEAL.assignTo(appBar.getScene());
//                    break;
//                case OTHER_VIEW:
//                    appBar.setNavIcon(MaterialDesignIcon.HTTPS.button(e -> appManager.switchView(OTHER_VIEW)));
//                    appBar.setTitleText("Other View");
//                    appBar.setVisible(true);
//                    break;
//            }
//        });
//        appManager.addViewFactory(OTHER_VIEW, () -> new View(new CheckBox("I like Glisten")));

    }

    @Override
    public void start(Stage primaryStage) throws IOException {
//        DisplayManager.setPrimaryStage(primaryStage);
//        primaryStage.initStyle(StageStyle.UNDECORATED);
//        primaryStage.setTitle("IM Platform");

        log.info("start application ");
        appManager.start(primaryStage);


//        CSSFX.start();
//        UserAgentBuilder.builder()
//                .themes(JavaFXThemes.MODENA)
//                .themes(MaterialFXStylesheets.forAssemble(true))
//                .setDeploy(true)
//                .setResolveAssets(true)
//                .build()
//                .setGlobal();

//        primaryStage.initStyle(StageStyle.UNDECORATED);
//
//
//        primaryStage.setResizable(true);

        FileIconUtil.setStageIcon(primaryStage);
        initDrag(primaryStage);
//
        primaryStage.setFullScreen(false);
        primaryStage.setWidth(800);
        primaryStage.setHeight(600);
        primaryStage.centerOnScreen();

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