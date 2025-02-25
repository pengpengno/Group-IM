package com.github.im.group.gui.util;

import lombok.SneakyThrows;
import org.scenicview.utils.PropertiesUtils;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

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


    @SneakyThrows
    public static void main(String[] args) {
        Locale english = new Locale("en", "US");
//        Locale chinese = new Locale("zh", "CN");
        Locale chinese = Locale.CHINA;



        // 设置中文环境
//        Locale chinese = Locale.CHINA;

        // 使用自定义的 Control 来获取 UTF-8 编码的资源
        var control = new UTF8Control();
        ResourceBundle bundle = ResourceBundle.getBundle("i18n.login", chinese, control);


        System.out.println( bundle.getString("login.text")); // 输出 "Hello"


        // 读取值
        System.out.println(bundle.getString("login.text"));

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n.login");
        messageSource.setDefaultEncoding("UTF-8"); // 关键设置



//        new PropertyResourceBundle()

        ResourceBundle englishBundle = ResourceBundle.getBundle("i18n.login");
//        ResourceBundle englishBundle = ResourceBundle.getBundle("login", english);
        ResourceBundle chineseBundle = ResourceBundle.getBundle("i18n.login", chinese);


        System.out.println( bundle.getString("login.text")); // 输出 "Hello"

        System.out.println( messageSource.getMessage("login.text", null, english)); // 输出 "Hello"
        System.out.println(englishBundle.getString("login.text")); // 输出 "Hello"
        System.out.println(chineseBundle.getString("login.text")); // 输出 "你好"

    }

}