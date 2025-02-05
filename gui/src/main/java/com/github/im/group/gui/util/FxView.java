package com.github.im.group.gui.util;

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
     * {@link FxmlLoader#FXML_PATH_FLODER 默认 fxml 路径}
     */
    public String path() ;

    /**
     *  窗体标题
     */
    public String title() default "IM";

    /**
     * 用于标识、检索 窗体的 ViewName
     * {@link com.gluonhq.charm.glisten.application.AppManager#switchView(String) 切换窗体}
     */
    public String viewName() ;
}
