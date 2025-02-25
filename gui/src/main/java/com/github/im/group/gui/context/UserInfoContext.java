package com.github.im.group.gui.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.github.im.common.connect.model.proto.Account;
import com.github.im.dto.user.UserInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Optional;

/**
 * Description:
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2024/11/7
 */
public class UserInfoContext {

    private static final TransmittableThreadLocal<UserInfo> currentUserThreadLocal = new TransmittableThreadLocal<>();


    private static final Sinks.Many<UserInfo> userInfoSink  = Sinks.many().multicast().onBackpressureBuffer();;


    // 设置当前用户
    public static void setCurrentUser(UserInfo user) {
        currentUserThreadLocal.set(user);
        userInfoSink.tryEmitNext(user);
    }

    /**
     * 订阅用户信息
     * @return 返回用户信息Flux流
     */
    public static Flux<UserInfo> subscribeUserInfoSink() {
        return userInfoSink.asFlux();
    }

    // 获取当前用户
    public static UserInfo getCurrentUser() {
        return currentUserThreadLocal.get();
    }

    public static Account.AccountInfo getAccountInfo() {


        return Optional.ofNullable(getCurrentUser()).map(userInfo -> {
            // 获取当前用户信息
            // ...
            Account.AccountInfo accountInfo = Account.AccountInfo.newBuilder()
                    .setUserId(userInfo.getUserId())
                    .setAccount(userInfo.getUsername())
                    .setAccountName(userInfo.getUsername())
                    .setEMail(userInfo.getEmail())
                    .build();
            return accountInfo;
        }).orElse( Account.AccountInfo.newBuilder().build());

    }

    // 清除当前用户
    public static void clear() {
        currentUserThreadLocal.remove();
    }
}