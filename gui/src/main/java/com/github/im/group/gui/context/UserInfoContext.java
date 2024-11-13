package com.github.im.group.gui.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.github.im.dto.user.UserInfo;

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




    // 设置当前用户
    public static void setCurrentUser(UserInfo user) {
        currentUserThreadLocal.set(user);
    }

    // 获取当前用户
    public static UserInfo getCurrentUser() {
        return currentUserThreadLocal.get();
    }

    // 清除当前用户
    public static void clear() {
        currentUserThreadLocal.remove();
    }
}