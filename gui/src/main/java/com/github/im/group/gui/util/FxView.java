package com.github.im.group.gui.util;

import com.github.im.group.gui.controller.PlatformView;

import java.lang.annotation.*;

/**
 * Description: [注解在 javafx 的 controller 上]
 * <p>
 *     用于标识 一些 fx 窗体的属性
 * </p>
 *
 * @author [peng]
 * @since 18
 */

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FxView {

    /**
     *  窗体的fxml 文件加载路径
     *  会判断当前平台根据 {@link PlatformView#getPlatform()}  平台} 加载在
     *  src/main/resources/fxml/desktop 或者 src/main/resources/fxml/mobile
     * {@link FxmlLoader#FXML_PATH_FLODER 默认 fxml 路径}
     */
    public  String path() default "fxml/";

    /**
     *  窗体的fxml 文件加载路径
     *  会判断当前平台根据 {@link PlatformView#getPlatform() 平台} 加载在
     *  src/main/resources/fxml/desktop 或者 src/main/resources/fxml/mobile
     * {@link FxmlLoader#FXML_PATH_FLODER 默认 fxml 路径}
     */
    public String fxmlName();

    /**
     *  窗体标题
     */
    public String title() default "IM";



    /**
     *  窗体的fxml 文件加载路径
     *  会判断当前平台根据 {@link PlatformView.PlatformType 平台} 加载在
     *  src/main/resources/fxml/desktop 或者 src/main/resources/fxml/mobile
     * {@link FxmlLoader#FXML_PATH_FLODER 默认 fxml 路径}
     */
//    public String viewName() ;
}
