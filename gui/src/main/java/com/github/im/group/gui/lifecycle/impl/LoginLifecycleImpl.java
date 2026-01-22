package com.github.im.group.gui.lifecycle.impl;

import com.github.im.common.connect.connection.client.ClientToolkit;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.dto.user.LoginRequest;
import com.github.im.dto.user.UserInfo;
import com.github.im.group.gui.views.AppViewManager;
import com.github.im.group.gui.config.SecureSettings;
import com.github.im.group.gui.config.ServerConnectProperties;
import com.github.im.group.gui.context.UserInfoContext;
import com.github.im.group.gui.controller.PlatformView;
import com.github.im.group.gui.lifecycle.LoginLifecycle;
import com.github.im.group.gui.views.MainPresenter;
import javafx.application.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class LoginLifecycleImpl implements LoginLifecycle {

    private final ServerConnectProperties serverConnectProperties;

    @Override
    public void preSendLogin(LoginRequest request) {
        log.debug("send login request ");
    }

    @Override
    public void loginCallBack(final UserInfo userInfo) {
        // On successful login, navigate to the main view
        Platform.runLater(() -> {
            // 存储凭据
            final var username = userInfo.getUsername();
            final var refreshToken = userInfo.getRefreshToken();
            SecureSettings.saveUserName(username);
            SecureSettings.saveSecretToken(refreshToken);

            log.debug("login success {}" ,userInfo);

            UserInfoContext.setCurrentUser(userInfo);
            var presenter = AppViewManager.switchView(MainPresenter.class);
            MainPresenter mainPresenter = (MainPresenter) presenter;

        });

        connectToServer(userInfo);

    }


    // 抛出网络异常
    private void connectToServer (UserInfo userInfo)   {

        try{
            var serverConnectPro = serverConnectProperties.getConnect();

            ClientToolkit.clientLifeStyle()
                    .connect(new InetSocketAddress(serverConnectPro.getHost()
                            , serverConnectPro.getPort()));

            var accountInfo = User.UserInfo.newBuilder()
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