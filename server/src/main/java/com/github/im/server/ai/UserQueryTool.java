package com.github.im.server.ai;

import org.springframework.stereotype.Component;

/**
 * 用户查询工具示例
 */
@Component
public class UserQueryTool {
    
    public String queryUserInfo(String userId) {
        // 这里应该是实际的用户信息服务调用
        return "User info for ID: " + userId;
    }
    
    public String queryUserPermissions(String userId) {
        // 这里应该是实际的权限查询服务调用
        return "Permissions for user: " + userId;
    }
}