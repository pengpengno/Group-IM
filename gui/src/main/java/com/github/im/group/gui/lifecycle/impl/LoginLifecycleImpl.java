package com.github.im.group.gui.lifecycle.impl;

import com.github.im.common.connect.connection.client.ClientToolkit;
import com.github.im.common.connect.model.proto.Account;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.dto.user.LoginRequest;
import com.github.im.dto.user.UserInfo;
import com.github.im.group.gui.config.ServerConnectProperties;
import com.github.im.group.gui.context.UserInfoContext;
import com.github.im.group.gui.controller.Display;
import com.github.im.group.gui.controller.MainHomeView;
import com.github.im.group.gui.controller.PlatformView;
import com.github.im.group.gui.controller.desktop.DesktopLoginView;
import com.github.im.group.gui.controller.desktop.DesktopMainView;
import com.github.im.group.gui.lifecycle.LoginLifecycle;
import javafx.application.Platform;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;

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

    @Autowired
    private ServerConnectProperties serverConnectProperties;


    @Override
    public void preSendLogin(LoginRequest request) {
        log.debug("send login request ");
    }

    @Override
    public void loginCallBack(UserInfo userInfo) {
        // On successful login, navigate to the main view
        Platform.runLater(() -> {
            log.debug("login success {}" ,userInfo);

            UserInfoContext.setCurrentUser(userInfo);
            var primaryStage = Display.getPrimaryStage();

            Display.display(MainHomeView.class);
            primaryStage.setMinWidth(560);
            primaryStage.setWidth(970);
            primaryStage.setHeight(560);
            primaryStage.setMinHeight(450);
        });

        try{
            var serverConnectPro = serverConnectProperties.getConnect();

            ClientToolkit.clientLifeStyle()
                    .connect(new InetSocketAddress(serverConnectPro.getHost()
                    , serverConnectPro.getPort()));

            var accountInfo = Account.AccountInfo.newBuilder()
                    .setPlatformType(PlatformView.getCurrentPlatformType())
                    .setUserId(userInfo.getUserId())
                    .setAccountName(userInfo.getUsername())
                    .setAccount(userInfo.getUsername())
                    .setEMail(userInfo.getEmail())
                    .build();

            var baseMessage = BaseMessage.BaseMessagePkg.newBuilder()
                    .setAccountInfo(accountInfo)
                    .build();

            ClientToolkit.reactiveClientAction()
                    .sendMessage(baseMessage)
                    .subscribe();

        }catch (Exception exception){

            log.error("connect to  server error " ,exception);

        }

    }
}