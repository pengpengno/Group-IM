package com.github.im.group.gui.lifecycle;


import com.github.im.dto.user.LoginRequest;
import com.github.im.dto.user.UserInfo;
import reactor.core.publisher.Mono;

/**
 * Description:
 * <p>
 *
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2024/11/7
 */
public interface LoginLifecycle {

    /**
     * before send Login
     * @param request
     */
    public void preSendLogin(LoginRequest request);


    /**
     * call back for login
     * @param userInfo
     */
    public void loginCallBack (UserInfo userInfo) ;


}
