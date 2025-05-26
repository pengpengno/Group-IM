package com.github.im.group.gui.util;

import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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


    // TODO  实现热更新机制

    private static final UTF8Control UTF8_CONTROL = new UTF8Control();

    private static final ObjectProperty<Locale> currentLocale = new SimpleObjectProperty<>( Locale.getDefault());


    public static ObjectProperty<Locale> localeProperty() {
        return currentLocale;
    }

    public static Locale getLocale() {
        return currentLocale.get();
    }

    public static void setLocale(Locale locale) {
        currentLocale.set(locale);
    }


//    public static ResourceBundle getResourceBundle(String packageName,Locale locale) {
//
//    }


    /**
     * getResourceBundle by  packageName
     *
     * eg. <pre> {@code     private ResourceBundle bundle = ResourceBundle.getBundle("i18n.menu.button");}
     * @param packageName under resources
     * </pre>
     * @return ResourceBundle
     */
    public static ResourceBundle getResourceBundle(String packageName) {
        return ResourceBundle.getBundle(packageName, localeProperty().get(),UTF8_CONTROL);
    }


    public static InputStream getInputSteamByBundleName(ResourceBundle bundle , String bundleName)  {
       return  I18nUtil.class.getResourceAsStream("/" + bundle.getString(bundleName));
    }



    @SneakyThrows
    public static void main(String[] args) {
        Locale english = new Locale("en", "US");
//        Locale chinese = new Locale("zh", "CN");
        Locale locale = Locale.ENGLISH;

        // 设置中文环境
//        Locale chinese = Locale.CHINA;

        // 使用自定义的 Control 来获取 UTF-8 编码的资源
        var control = new UTF8Control();
//        ResourceBundle bundle = ResourceBundle.getBundle("i18n.contract.contract", locale, control);
        ResourceBundle bundle = getResourceBundle("i18n.contract.contract");


        System.out.println( bundle.getString("contract.DetailInfoPane.sendButton.text")); // 输出 "Hello"
//        System.out.println( bundle.getString("login.text")); // 输出 "Hello"


        // 读取值
//        System.out.println(bundle.getString("login.text"));

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n.login");
        messageSource.setDefaultEncoding("UTF-8"); // 关键设置



//        new PropertyResourceBundle()

        ResourceBundle englishBundle = ResourceBundle.getBundle("i18n.login");
//        ResourceBundle englishBundle = ResourceBundle.getBundle("login", english);
        ResourceBundle chineseBundle = ResourceBundle.getBundle("i18n.login", locale);



        System.out.println( messageSource.getMessage("login.text", null, english)); // 输出 "Hello"
        System.out.println(englishBundle.getString("login.text")); // 输出 "Hello"
        System.out.println(chineseBundle.getString("login.text")); // 输出 "你好"

    }


    public static ResourceBundle getBundle() {
        return ResourceBundle.getBundle("i18n.messages", getLocale());
    }


    private StringBinding createBindingForKey(String key) {
        return new StringBinding() {
            {
                bind(localeProperty());
            }

            @Override
            protected String computeValue() {
                return getBundle().getString(key);
            }
        };
    }
}