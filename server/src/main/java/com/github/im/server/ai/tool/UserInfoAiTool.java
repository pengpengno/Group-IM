package com.github.im.server.ai.tool;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 用户信息AI工具 - 使用Spring AI的工具注解
 */
@Component
public class UserInfoAiTool {

    private final UserInfoTool userInfoTool;

    @Autowired
    public UserInfoAiTool(UserInfoTool userInfoTool) {
        this.userInfoTool = userInfoTool;
    }

    /**
     * 获取用户信息
     */
    public String getUserInfo(String userId) {
        return userInfoTool.getUserInfo(userId);
    }

    /**
     * 获取用户联系信息
     */
    public String getUserContactInfo(String userId) {
        return userInfoTool.getUserContactInfo(userId);
    }
}