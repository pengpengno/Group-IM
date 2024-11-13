package com.github.im.group.gui.lifecycle.impl;

import com.github.im.dto.user.LoginRequest;
import com.github.im.dto.user.UserInfo;
import com.github.im.group.gui.context.UserInfoContext;
import com.github.im.group.gui.controller.Display;
import com.github.im.group.gui.controller.MainController;
import com.github.im.group.gui.lifecycle.LoginLifecycle;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Description:
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2024/11/7
 */

@Service
@Slf4j
public class LoginLifecycleImpl implements LoginLifecycle {


    @Override
    public void preSendLogin(LoginRequest request) {

    }

    @Override
    public void loginCallBack(UserInfo userInfo) {

        // On successful login, navigate to the main view
        Platform.runLater(() -> {
            log.debug("login success {}" ,userInfo);
            UserInfoContext.setCurrentUser(userInfo);
            Display.display(MainController.class);
        });


    }
}