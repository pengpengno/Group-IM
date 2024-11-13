package com.github.im.group.gui.lifecycle;/**
 * Description: [描述方法或类的作用和功能]
 * <p>
 * </p>
 *
 * @author [peng]
 * @since 07
 */

import com.github.im.dto.user.LoginRequest;
import com.github.im.dto.user.UserInfo;

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
