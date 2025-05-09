package com.github.im.group.gui.controller;/**
 * Description: [描述方法或类的作用和功能]
 * <p>
 * </p>
 *
 * @author [peng]
 * @since 06
 */

import com.github.im.common.connect.enums.PlatformType;
import com.github.im.group.gui.util.FxView;
import com.github.im.group.gui.util.FxmlLoader;
import com.github.im.group.gui.views.AppViewManager;
import com.gluonhq.charm.glisten.afterburner.AppViewRegistry;
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.mvc.View;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Description:
 * <p>
 *     display windows with corresponding platform fxml
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2024/11/6
 */
//@Component
@Slf4j
public class DisplayManager {


    @Getter
    @Setter
    private static Stage primaryStage;

    public static Stage getPrimaryStage() {
        var instance = AppManager.getInstance();
        return primaryStage;
    }

    @Getter
    @Setter
    private static Scene primaryScene;
    /**
     * postInit
     */
    private static Consumer<Scene> postInit ;

    private static final String DESKTOP_PATH = "desktop/";
    private static final String FXML_PATH  = "fxml/";
    private static final String MOBILE_PATH = "mobile/";
    private static final String FXML_SUFFIX = ".fxml";

    /**
     * 存放 对应的 Gluon 中的展示图层
     */
    public static final ConcurrentHashMap<String,View> DISPLAY_VIEW_MAP = new ConcurrentHashMap<>();

    /**
     * 缓存 FXML 加载后的 Scene 节点
     */
    public static final ConcurrentHashMap<Class<?>, Scene> DISPLAY_SCENE_MAP = new ConcurrentHashMap<>();

    /**
     * 存放 页面的控制器 Controller
     */
    public static final ConcurrentHashMap<Class<?>,Object> DISPLAY_CONTROLLER_MAP = new ConcurrentHashMap<>();





    public  static <T extends PlatformView> T getController(Class<T> displayClass) {
        return AppViewManager.getPresenter(displayClass);
//        if (DISPLAY_CONTROLLER_MAP.containsKey(displayClass)) {
//            return (T) DISPLAY_CONTROLLER_MAP.get(displayClass);
//        }
//        return registerView(displayClass);
    }


    /**
     * 注册一个平台视图
     * 此方法用于注册一个特定平台的视图，通过加载FXML文件并创建视图实例
     *
     * @param displayClass 视图类，必须带有FxView注解
     * @param <T> 视图类的类型，继承自PlatformView
     * @return 返回创建的视图实例，如果注册失败则返回null
     */
    public static <T extends PlatformView> T registerView(Class<T> displayClass) {

        // 获取当前平台信息
        var currentPlatform = com.gluonhq.attach.util.Platform.getCurrent();
        var platformType = PlatformView.getPlatformType(currentPlatform);

        log.debug("Current platform is {} ,platformType {} ", currentPlatform, platformType);

        // 获取 FxView 注解
        var annotation = displayClass.getAnnotation(FxView.class);
        if (annotation == null) {
            log.warn("No FxView annotation found for class {}", displayClass.getName());
            return null;
        }

        // 构建 FXML 文件路径
        String fxmlLoaderPath = buildFxmlPath(platformType, annotation.fxmlName());

        // 加载 FXML 文件和其控制器
        var tuples = FxmlLoader.loadFxml(fxmlLoaderPath);
//        var tuples = fxmlLoader.loadFxml(fxmlLoaderPath);
        if (tuples == null) {
            log.error("Failed to load FXML file: {}", fxmlLoaderPath);
            return null;
        }

        // 获取父节点和控制器
        var parent = tuples.getT2();
        var controller = tuples.getT1().getController();

        if (controller == null) {
            log.error("Loaded FXML does not contain a valid parent or controller.");
            return null;
        }

        // 创建并注册视图
        var viewName = displayClass.getName();

        if (!DISPLAY_VIEW_MAP.containsKey(viewName)) {
            try {
                View view = new View(parent);

                var instance = AppManager.getInstance();
                if(instance!= null){
                    instance.addViewFactory(viewName, () -> view);
                }
                DISPLAY_VIEW_MAP.putIfAbsent(viewName, view);
                log.info("View registered successfully: {}", viewName);
            } catch (Exception e) {
                log.error("Failed to register view {}: ", viewName, e);
                return null;
            }
        }

//        if (!DISPLAY_SCENE_MAP.containsKey(displayClass)) {
//            try {
//                var value = new Scene(parent);
//                DISPLAY_SCENE_MAP.putIfAbsent(displayClass, value);
//
//                log.info("View registered successfully: {}", viewName);
//            } catch (Exception e) {
//                log.error("Failed to register view {}: ", viewName, e);
//                return null;
//            }
//        }
        var controllerObj = (T) controller;

        DISPLAY_CONTROLLER_MAP.putIfAbsent(displayClass,controllerObj);
        return controllerObj;
    }

    /**
     * 构建平台特定的 FXML 文件路径
     */
    private static String buildFxmlPath(PlatformType platformType, String fxmlName) {
        String platformPath = (platformType == PlatformType.DESKTOP) ? DESKTOP_PATH : MOBILE_PATH;
        return FXML_PATH + platformPath + fxmlName + FXML_SUFFIX;
    }

    /**
     *
     * @param displayClass
     */
    public static void start(Class<? extends PlatformView> displayClass) {
        registerView(displayClass);
        start();
    }

    public static void start() {
        Platform.runLater(() -> {
            var primaryStage = getPrimaryStage();
            if (primaryStage != null) {
                var primaryScene = getPrimaryScene();
                if (primaryScene != null){
                    postInit.accept(primaryScene);
                }
                if (!primaryStage.isShowing()){
                    primaryStage.show();
                }
            }
        });
    }


    /**
     * 切换 view
     *  不存在 的View 就先register 在  switch
     * @param displayClass 展示的view
     */
    static void switchView(Class<? extends PlatformView> displayClass){
        if (!DISPLAY_VIEW_MAP.containsKey(displayClass.getName())){
            registerView(displayClass);
        }

        AppManager.getInstance().switchView(displayClass.getName());
    }
//
//    static void switchView(Class<? extends PlatformView> displayClass){
//        final boolean isFirst = primaryStage == null;
//
//        if (!DISPLAY_SCENE_MAP.containsKey(displayClass)){
//            registerView(displayClass);
//        }
//        var scene = DISPLAY_SCENE_MAP.get(displayClass);
//
//        if( postInit != null){
//            postInit.accept(scene);
//        }
//
//        var primaryStage = getPrimaryStage();
//        primaryStage.setScene(scene);
//        setPrimaryScene(scene);
//
//        if (!primaryStage.isShowing()){
//            primaryStage.show();
//        }
//
//        /**
//         * Gluon 切换视图
//         */
//        AppManager.getInstance().switchView(displayClass.getName());
//
//    }

    public static void initialize(Consumer<Scene> postInit) {
        DisplayManager.postInit = postInit;
    }

    /**
     * 展示当前窗体
     * 此处使用来渲染 窗体
     * @param displayClass
     */
    public static void display(Class<? extends PlatformView> displayClass){
        Platform.runLater(()-> {
            switchView(displayClass);
        });
    }

}
