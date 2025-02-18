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
import com.github.im.group.gui.util.StageManager;
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.mvc.View;
import jakarta.annotation.Resource;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.bootstrapfx.BootstrapFX;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
@Component
@Slf4j
public class Display   {
//public class Display  implements ApplicationContextAware {



    @Getter
    @Setter
    private static Stage primaryStage;

    private static final String DESKTOP_PATH = "desktop/";
    private static final String FXML_PATH  = "fxml/";
    private static final String MOBILE_PATH = "mobile/";
    private static final String FXML_SUFFIX = ".fxml";

    public static final ConcurrentHashMap<String,View> DISPLAY_VIEW_MAP = new ConcurrentHashMap<>();



    public  static <T extends PlatformView> T registerView(Class<T> displayClass) {



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
                AppManager.getInstance().addViewFactory(viewName, () -> view);
                log.info("View registered successfully: {}", viewName);
            } catch (Exception e) {
                log.error("Failed to register view {}: {}", viewName, e.getMessage());
                return null;
            }
        }
        return (T) controller;
    }

    /**
     * 构建平台特定的 FXML 文件路径
     */
    private static String buildFxmlPath(PlatformType platformType, String fxmlName) {
        String platformPath = (platformType == PlatformType.DESKTOP) ? DESKTOP_PATH : MOBILE_PATH;
        return FXML_PATH + platformPath + fxmlName + FXML_SUFFIX;
    }

    /**
     * 切换 view
     *  不存在 的View 就先register 在  switch
     * @param displayClass 展示的view
     */

    public static  void switchView(Class<? extends PlatformView> displayClass){
        if (!DISPLAY_VIEW_MAP.containsKey(displayClass.getName())){
            registerView(displayClass);
        }
        AppManager.getInstance().switchView(displayClass.getName());
    }
    /**
     * 展示当前窗体
     * 此处使用的事 Gluon 的组件来渲染 窗体
     * @param displayClass
     */
    public static void display(Class<? extends PlatformView> displayClass){
        Platform.runLater(()-> {
            switchView(displayClass);
        });
    }

}
