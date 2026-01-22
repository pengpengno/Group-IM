package com.github.im.common.connect.connection;

import com.github.im.common.connect.model.proto.User;
import io.netty.util.AttributeKey;

/**
 * @author pengpeng
 * @description
 * @date 2023/3/16
 */
public interface ConnectionConstants {

    public static AttributeKey<User.UserInfo> BING_ACCOUNT_KEY = AttributeKey.valueOf("User");




}
