package com.github.im.group.gui.util;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Description:
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/2/18
 */
public class I18nUtil {

    public static void main(String[] args) {
        Locale english = new Locale("en", "US");
        Locale chinese = new Locale("zh", "CN");

        ResourceBundle englishBundle = ResourceBundle.getBundle("Messages", english);
        ResourceBundle chineseBundle = ResourceBundle.getBundle("Messages", chinese);

        System.out.println(englishBundle.getString("greeting")); // 输出 "Hello"
        System.out.println(chineseBundle.getString("greeting")); // 输出 "你好"

    }
}