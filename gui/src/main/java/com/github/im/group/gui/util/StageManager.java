package com.github.im.group.gui.util;

import javafx.stage.Stage;

/**
 * Description:
 * <p>
 *    主Stage 管理器
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2024/10/18
 */
public class StageManager {
    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }
}
