package com.github.im.server.ai;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 聊天历史工具示例
 */
@Component
public class ChatHistoryTool {
    
    public List<String> getChatHistory(String sessionId, int limit) {
        // 这里应该是实际的聊天历史查询服务调用
        return List.of("Sample message 1", "Sample message 2");
    }
    
    public String getRecentConversation(String userId, String targetUserId) {
        // 这里应该是实际的最近对话查询服务调用
        return "Recent conversation between " + userId + " and " + targetUserId;
    }
}