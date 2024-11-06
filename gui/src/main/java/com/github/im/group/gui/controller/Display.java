package com.github.im.group.gui.controller;/**
 * Description: [描述方法或类的作用和功能]
 * <p>
 * </p>
 *
 * @author [peng]
 * @since 06
 */

import com.github.im.group.gui.util.FxmlLoader;
import com.github.im.group.gui.util.StageManager;
import javafx.application.Platform;

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
public interface Display {


    public static  void display(Class displayClass){

        Platform.runLater(()-> {
//            Class<? extends Display> displayClass = this.getClass();
            var scene = FxmlLoader.getSceneInstance(displayClass);
            var primaryStage = StageManager.getPrimaryStage();
            primaryStage.sizeToScene(); // 自动调整主 Stage 大小以适应当前 Scene 的大小

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
