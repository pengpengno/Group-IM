package com.github.im.group.gui.util;

import javafx.scene.text.Font;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FontRegistrar {
    public static void registerFonts() {
//        loadFont("com/gluonhq/charm/glisten/control/MaterialIcons-Regular.ttf");
//        loadFont("com/gluonhq/charm/glisten/control/Roboto-Regular.ttf");
//        loadFont("com/gluonhq/charm/glisten/control/Roboto-Bold.ttf");
//        loadFont("com/gluonhq/charm/glisten/control/Roboto-Medium.ttf");
    }

    private static void loadFont(String resourcePath) {
        Font font = Font.loadFont(FontRegistrar.class.getResourceAsStream(resourcePath), 12);
        if (font != null) {
            log.info("Loaded font: " + font.getName());
        } else {
            log.warn("Failed to load font: " + resourcePath);
        }
    }
}
