package com.github.im.group.gui.views;

import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.application.ViewStackPolicy;
import com.gluonhq.charm.glisten.control.NavigationDrawer;

/**
 * Description:
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/5/16
 */
public interface ViewLifeCycle {

    /**
     * 唯一标识 , 用于在Registry中查找
     * @return String
     */
    String getId();

    /**
     * 向 {@link AppViewManager }
     */
    void registerView();

    /**
     * 获取用于侧边菜单栏的 Item
     * @return NavigationDrawer.Item
     */
    public NavigationDrawer.Item getMenuItem() ;

    /**
     * 是否 Drawer 中展示
     * @return
     */
    default public boolean isShownInDrawer(){
        return false;
    }

    /**
     * 切换 View
     * @return 切换视图至 当前View 并且 返回 presenter 对象
     */
    default Object switchView(){
        AppManager.getInstance().switchView(getId(), getViewStackPolicy());
        return getPresenter();
    }

    /**
     * 返回 Presenter 对象
     * @return Presenter 对象
     */
    Object getPresenter();


    ViewStackPolicy getViewStackPolicy();


    /**
     * 获取 ViewRegistry {@link AppViewManager#REGISTRY}
     * @return ViewRegistry
     */
    default ViewRegistry getRegistry(){
        return ViewRegistry.getInstance();
    }


    /**
     * View 的 一些 Flag 类型
     */
    public static enum Flag {
        HOME_VIEW, // 主页
        SHOW_IN_DRAWER, // 在 菜单展示
        SKIP_VIEW_STACK; // 堆栈行为策略

        private Flag() {
        }
    }

}