/*
 * Copyright (c) 2017, 2021, Gluon
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of Gluon, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL GLUON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.im.group.gui.views;

import com.github.im.group.gui.controller.PlatformView;
import com.github.im.group.gui.util.ViewUtils;
import com.gluonhq.charm.glisten.application.AppManager;
import com.gluonhq.charm.glisten.control.Avatar;
import com.gluonhq.charm.glisten.control.NavigationDrawer;
import com.gluonhq.charm.glisten.visual.MaterialDesignIcon;
import javafx.scene.Node;
import javafx.scene.image.Image;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.github.im.group.gui.views.ViewLifeCycle.Flag.*;


/**
 * 官方 Demo 代码 示例
 */
public class AppViewManager {


    private static final ViewRegistry REGISTRY = ViewRegistry.getInstance();

    private static final ResourceBundle bundle = ResourceBundle.getBundle("i18n.drawer");


    /**
     * 创建 {@link BView beanView 用于将bean 注册为View}
     * @param presenter
     * @return
     */
    public static ViewLifeCycle createView(Object presenter) throws IllegalArgumentException {
        var bView = new BView(presenter);

        return bView;
    }

    /**
     * 注册主界面
     */
    public static FView createHomeView(Class<? extends PlatformView> presenterClass) {

        var home = MaterialDesignIcon.HOME;
        ViewLifeCycle.Flag[] flags = new ViewLifeCycle.Flag[]{HOME_VIEW,SHOW_IN_DRAWER, SKIP_VIEW_STACK};

        var view = REGISTRY.createView( presenterClass, home, flags);

        return view;
    }

    /**
     * 注册主界面
     */
    public static FView createView(Class<? extends PlatformView> presenterClass, MaterialDesignIcon menuIcon ) {

        FView.Flag[] flags = new FView.Flag[]{SHOW_IN_DRAWER, SKIP_VIEW_STACK};

        var view = REGISTRY.createView( presenterClass, menuIcon, flags);

        return view;
    }

    /**
     * 注册主界面
     */
    public static FView createView(Class<? extends PlatformView> presenterClass) {
        var ICON = MaterialDesignIcon.ACCESS_ALARM;
        return createView(presenterClass, ICON);
    }


    public static Object switchView(String viewId ){

        return  REGISTRY.getView(viewId).map(view -> {
            return Optional.ofNullable(view.switchView())
                    .orElseThrow(()-> new RuntimeException("View not found" + viewId));
        }).orElseThrow(()-> new RuntimeException("View not found" + viewId));
    }

    /**
     * 这里 viewclass 使用 getClass().getSimpleName() 获取视图名称
     * @param viewclass 需要切换的视图 Presenter 的Class
     * @return 返回View 的控制器实体
     */
    public static Object switchView(Class<?> viewclass ){

        return REGISTRY.getView(viewclass.getSimpleName())
                .map(view -> {
            return Optional.ofNullable(view.switchView())
                    .orElseThrow(()-> new RuntimeException("View not found" + viewclass.getSimpleName()))
                    ;
        }).orElseThrow(()-> new RuntimeException("View not found" + viewclass.getSimpleName()));
    }

//    public static <T extends PlatformView>  T getPresenter(Class<T> viewclass ) {
    public static <T>  T getPresenter(Class<T> viewclass ) {
        return REGISTRY.getView(viewclass.getSimpleName()).map(view -> {
            return (T)view.getPresenter();
        }).orElseThrow(()-> new RuntimeException("View not found" + viewclass.getSimpleName()));
    }


    /**
     * 注册所有视图
     */
    public static void registerViewsAndDrawer() {
        for (ViewLifeCycle view : REGISTRY.getViews()) {
            view.registerView();
        }

        NavigationDrawer.Header header = new NavigationDrawer.Header(bundle.getString("drawer.header.title"),
                bundle.getString("drawer.header.description"),
                new Avatar(21,
                        new Image(Objects.requireNonNull(AppViewManager.class.getResourceAsStream("/images/icon.png")))));
        header.setStyle("-fx-background-color: white;");

        var drawer = AppManager.getInstance().getDrawer();
        ViewUtils.buildDrawer(drawer, header, REGISTRY.getViews());
    }
}
