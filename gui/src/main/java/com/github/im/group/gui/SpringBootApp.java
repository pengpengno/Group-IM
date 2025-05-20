package com.github.im.group.gui;

import com.github.im.group.gui.api.UserEndpoint;
import com.github.im.group.gui.controller.desktop.chat.ChatMainPresenter;
import com.github.im.group.gui.util.ViewUtils;
import com.github.im.group.gui.views.AppViewManager;
import org.scenicview.view.ScenegraphTreeView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;

@SpringBootApplication(exclude = {
//        WebMvcAutoConfiguration.class
})
public class SpringBootApp {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootApp.class, args);
    }
}
