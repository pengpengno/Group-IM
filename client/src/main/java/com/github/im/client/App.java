//package com.github.im.client;
//
//import com.gluonhq.charm.glisten.application.AppManager;
//import com.gluonhq.charm.glisten.visual.Swatch;
//import javafx.application.Application;
//import javafx.scene.Scene;
//import javafx.scene.image.Image;
//import javafx.stage.Stage;
//
///**
// * Description:
// * <p>
// * </p>
// *
// * @author pengpeng
// * @version 1.0
// * @since 2025/5/8
// */
//public class App extends Application {
//    public static final String POPUP_FILTER_NOTES = "Filter Notes";
//    private final AppManager appManager = AppManager.initialize(this::postInit);
//
//    @Override
//    public void init() {
//        AppViewManager.registerViewsAndDrawer();
//    }
//
//    @Override
//    public void start(Stage stage) {
//        appManager.start(stage);
//    }
//
//    private void postInit(Scene scene) {
//        Swatch.LIGHT_GREEN.assignTo(scene);
//
//        scene.getStylesheets().add(Notes.class.getResource("style.css").toExternalForm());
//        ((Stage) scene.getWindow()).getIcons().add(new Image(Notes.class.getResourceAsStream("/icon.png")));
//    }
//
//    public static void main(String[] args) {
//        launch(args);
//    }
//}