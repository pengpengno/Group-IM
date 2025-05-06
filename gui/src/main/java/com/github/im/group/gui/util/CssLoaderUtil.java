package com.github.im.group.gui.util;

import javafx.scene.Scene;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.net.URL;

public class CssLoaderUtil {

    /**
     * 加载指定的 CSS 文件并将其应用到场景中。
     *
     * @param scene  要应用 CSS 的场景
     * @param cssPath CSS 文件的路径
     */
    public static void loadCss(Scene scene, String cssPath) {
//        URL cssResource = CssLoaderUtil.class.getResource(cssPath);
        try {
            var url = new ClassPathResource(cssPath).getURL();
            scene.getStylesheets().add(url.toExternalForm());

        } catch (IOException e) {
//            throw new RuntimeException(e);
            System.err.println("CSS file not found: " + cssPath);

        }
//        if (cssResource != null) {
//            scene.getStylesheets().add(cssResource.toExternalForm());
//        } else {
//            System.err.println("CSS file not found: " + cssPath);
//        }
    }
}
