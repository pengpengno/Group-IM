package com.github.im.group.gui.views;/**
 * Description: [描述方法或类的作用和功能]
 * <p>
 * </p>
 *
 * @author [peng]
 * @since 16
 */

import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.scene.Node;

/**
 * Description: 用于设定 Presenter 在菜单栏的属性
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/5/16
 */
public interface MenuItem {



    default String title() {
        return "menu";
    }


    /**
     * 默认的菜单栏的行为状态
     * @return
     */
    default ViewLifeCycle.Flag[] flags() {
        return new ViewLifeCycle.Flag[]{};
    }


    /**
     * 菜单栏的Icon 图标
     * @return Node
     */
    default Node menuIcon() {
        return MaterialDesignIcon.HOME.graphic();
    }
}
