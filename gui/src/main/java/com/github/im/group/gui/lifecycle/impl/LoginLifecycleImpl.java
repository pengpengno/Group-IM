package com.github.im.group.gui.lifecycle.impl;

import com.github.im.common.connect.connection.client.ClientToolkit;
import com.github.im.common.connect.model.proto.Account;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.dto.user.LoginRequest;
import com.github.im.dto.user.UserInfo;
import com.github.im.group.gui.config.ServerConnectProperties;
import com.github.im.group.gui.context.UserInfoContext;
import com.github.im.group.gui.controller.Display;
import com.github.im.group.gui.controller.MainController;
import com.github.im.group.gui.lifecycle.LoginLifecycle;
import feign.Client;
import jakarta.annotation.Resource;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
            Display.display(MainController.class);
        });

        try{

            var serverConnectPro = serverConnectProperties.getConnect();
            ClientToolkit.clientLifeStyle()
                    .connect(new InetSocketAddress(serverConnectPro.getHost()
                    , serverConnectPro.getPort()));



            var accountInfo = Account.AccountInfo.newBuilder()
                    .setUserId(userInfo.getUserId())
                    .setAccountName(userInfo.getUsername())
                    .build();

            var baseMessage = BaseMessage.BaseMessagePkg.newBuilder()
                    .setAccountInfo(accountInfo)
                    .build();

            ClientToolkit.reactiveClientAction()
                    .sendMessage(baseMessage).subscribe();

        }catch (Exception exception){

            log.error("connect to  server error " ,exception);

        }



    }
}