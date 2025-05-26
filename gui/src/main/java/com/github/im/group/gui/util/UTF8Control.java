package com.github.im.group.gui.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * UTF-8 读取 properties 文件
 */
public class UTF8Control extends ResourceBundle.Control {
    @Override
    public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
            throws IOException {
        // 生成资源文件名，例如：i18n/login_zh.properties

//        String bundleName = toResourceName(baseName, "properties");

        String bundleName = toBundleName(baseName, locale);
        // 再转换成资源路径 i18n/login_zh_CN.properties
        String resourceName = toResourceName(bundleName, "properties");

        // 获取资源流
        InputStream stream = loader.getResourceAsStream(resourceName);
        if (stream == null) {
            return null;
        }

        // 使用 UTF-8 读取 properties
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return new PropertyResourceBundle(reader);
        }
    }

}
