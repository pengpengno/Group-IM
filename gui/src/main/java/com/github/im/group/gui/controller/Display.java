package com.github.im.group.gui.controller;/**
 * Description: [描述方法或类的作用和功能]
 * <p>
 * </p>
 *
 * @author [peng]
 * @since 06
 */

import com.github.im.group.gui.util.FxView;
import com.github.im.group.gui.util.FxmlLoader;
import com.github.im.group.gui.util.StageManager;
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.mvc.View;
import javafx.application.Platform;
import javafx.scene.Scene;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.bootstrapfx.BootstrapFX;

/**
 * Description:
 * <p>
 *     窗体的展示
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2024/11/6
 */
//@Slf4j
public interface Display {


    /**
     * 展示当前窗体
     * 此处使用的事 Gluon 的组件来渲染 窗体
     * @param displayClass
     */
    public static  void display(Class<?> displayClass){
        Platform.runLater(()-> {
            var appManager = AppManager.getInstance();

            var annotation = displayClass.getAnnotation(FxView.class);
            if (annotation != null){
                var viewName = annotation.viewName();
                try{
                    appManager.addViewFactory(viewName,()->{
                        var sceneInstance = FxmlLoader.getSceneInstance(displayClass);
                        if (sceneInstance != null){
                            return new View(sceneInstance.getRoot());
                        }
                        return new View();
                    });
                }catch (Exception exception){
                    exception.printStackTrace();
//                    log.error("already existed ",exception);
                }

                appManager.switchView(viewName);
            }

        });


    }

    /**
     * 展示当前窗体
     * 此方法使用的事原始的 Javafx 在 primaryStage 切换 Scene ，在
     * @param displayClass
     */
    @Deprecated
    public static  void displayV1(Class<?> displayClass){

        Platform.runLater(()-> {
//            Class<? extends Display> displayClass = this.getClass();
            var scene = FxmlLoader.getSceneInstance(displayClass);
            var primaryStage = StageManager.getPrimaryStage();
            primaryStage.sizeToScene(); // 自动调整主 Stage 大小以适应当前 Scene 的大小

//            scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());       //(3)

            scene.widthProperty().addListener((observable, oldValue, newValue) -> {
                primaryStage.setWidth(newValue.doubleValue());
            });
            scene.heightProperty().addListener((observable, oldValue, newValue) -> {
                primaryStage.setHeight(newValue.doubleValue());
            });


            primaryStage.setResizable(true);
            primaryStage.setScene(scene);
        });
    }
}
