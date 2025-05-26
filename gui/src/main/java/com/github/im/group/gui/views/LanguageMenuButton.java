package com.github.im.group.gui.views;

import com.github.im.group.gui.util.I18nUtil;
import com.gluonhq.charm.glisten.control.AppBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;

import java.util.Locale;

public class LanguageMenuButton extends MenuButton {

    public LanguageMenuButton(Runnable onLanguageChanged) {
        super("语言");
//        super(I18nUtil.getResourceBundle("menu.language"));

        MenuItem english = new MenuItem("English");
        english.setOnAction(e -> {
//            I18nContext.setLocale(Locale.ENGLISH);
            onLanguageChanged.run();
        });

        MenuItem chinese = new MenuItem("中文");
        chinese.setOnAction(e -> {
//            I18nContext.setLocale(Locale.SIMPLIFIED_CHINESE);
            onLanguageChanged.run();
        });

        getItems().addAll(english,chinese);
    }

    public void updateText() {
//        setText(I18nContext.get("menu.language"));
    }
}
