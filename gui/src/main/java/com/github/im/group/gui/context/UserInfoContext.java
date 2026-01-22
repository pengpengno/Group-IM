package com.github.im.group.gui.context;

import com.github.im.dto.user.UserInfo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Description:
 * <p>
 *     用户信息存储
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2024/11/7
 */
public class UserInfoContext {

//    private static final TransmittableThreadLocal<UserInfo> currentUserThreadLocal = new TransmittableThreadLocal<>();


    private static final Sinks.Many<UserInfo> userInfoSink  = Sinks.many().multicast().onBackpressureBuffer();;

    private static UserInfo userInfo;
    private static User.UserInfo accountInfo;


    static {
        userInfoSink.asFlux()
                .subscribe(userInfo -> {
                    UserInfoContext.userInfo = userInfo;
                    accountInfo = getAccountInfo(userInfo);
                });
    }
    // 设置当前用户
    public static void setCurrentUser(UserInfo user) {
//        currentUserThreadLocal.set(user);
        userInfo = user;
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
        return userInfo;
    }

    public static User.UserInfo getAccountInfo() {

        return accountInfo;
//        return Optional.ofNullable(getCurrentUser()).map(userInfo -> getAccountInfo(userInfo)).orElse( User.UserInfo.newBuilder().build());

    }

    private  static User.UserInfo getAccountInfo(UserInfo userInfo) {
        // 获取当前用户信息
        User.UserInfo accountInfo = User.UserInfo.newBuilder()
                .setUserId(userInfo.getUserId())
                .setAccount(userInfo.getUsername())
                .setAccountName(userInfo.getUsername())
                .setEMail(userInfo.getEmail())
                .build();
        return accountInfo;
    }
//
//    // 清除当前用户
//    public static void clear() {
//        currentUserThreadLocal.remove();
//    }
}