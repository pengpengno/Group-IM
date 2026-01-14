package com.github.im.server.ai.tool;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 用户信息查询工具 - 用于Spring AI工具调用示例
 */
@Component
public class UserInfoTool {

    public String getUserInfo(String userId) {
        // 实际实现中这里会调用用户服务获取真实用户信息
        return "User ID: " + userId + ", Name: Sample User, Department: Engineering, Position: Software Engineer";
    }

    public String getUserContactInfo(String userId) {
        // 实际实现中这里会调用用户服务获取真实联系信息
        return "Email: user@example.com, Phone: 123-456-7890, Extension: 1234";
    }
}