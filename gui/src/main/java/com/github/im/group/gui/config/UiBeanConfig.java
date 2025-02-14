package com.github.im.group.gui.config;

import com.github.im.group.gui.controller.Display;
import com.github.im.group.gui.controller.LoginView;
import com.github.im.group.gui.controller.MainHomeView;
import com.github.im.group.gui.controller.PlatformView;
import com.github.im.group.gui.controller.desktop.DesktopMainView;
import com.github.im.group.gui.util.FxmlLoader;
import com.gluonhq.charm.glisten.application.AppManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

/**
 *  register  controller bean into spring env
 *  and add View into Gluon {@link AppManager#addViewFactory(String, Supplier)}
 *
 * @author pengpeng
 * @version 1.0
 * @since 2025/2/14
 */
@Configuration
@Slf4j
public class UiBeanConfig {

//    @Autowired
//    private FxmlLoader fxmlLoader;
//
//    @Autowired
//    private Display display;


//    @Bean
//    public MainHomeView mainHomeView() {
//
//        return display.registerView(MainHomeView.class);
//
//    }
//
//
//
//    @Bean
//    @ConditionalOnBean(MainHomeView.class)
//    public LoginView loginView() {
//
//        var bean = display.registerView(LoginView.class);
//
//        return bean;
//
//    }


}