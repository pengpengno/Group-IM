package com.github.im.server.ai;

import org.springframework.stereotype.Component;

/**
 * AI工具机器人 - 支持工具调用的机器人
 */
@Component
public class AiToolBot implements BotHandler {

    private final AiChatService ai;
    private final ToolRegistry toolRegistry;
    private final PromptFactory promptFactory;

    public AiToolBot(AiChatService ai, ToolRegistry toolRegistry, PromptFactory promptFactory) {
        this.ai = ai;
        this.toolRegistry = toolRegistry;
        this.promptFactory = promptFactory;
    }

    @Override
    public boolean canHandle(Message msg) {
        String content = msg.getContent() != null ? msg.getContent().toLowerCase() : "";
        return content.startsWith("/tool") || 
               content.contains("user info") || 
               content.contains("permission") ||
               content.contains("history");
    }

    @Override
    public BotReply handle(Message msg, BotContext ctx) {
        String content = msg.getContent();
        
        // 简单的工具识别和调用逻辑
        if (content != null && content.startsWith("/tool")) {
            // 解析工具命令
            String[] parts = content.split("\\s+", 3);
            if (parts.length >= 2) {
                String toolName = parts[1];
                if (toolRegistry.hasTool(toolName)) {
                    Object[] params = parts.length > 2 ? new Object[]{parts[2]} : new Object[]{};
                    Object result = toolRegistry.invokeTool(toolName, params);
                    return new BotReply(result.toString());
                } else {
                    return new BotReply("Unknown tool: " + toolName);
                }
            }
        }
        
        // 对于包含特定关键词的消息，尝试调用相应工具
        String response;
        if (content != null && content.toLowerCase().contains("user info")) {
            // 调用用户查询工具
            Object result = toolRegistry.invokeTool("userQuery", new Object[]{"sample_user_id"});
            response = "User Info: " + result.toString();
        } else if (content != null && content.toLowerCase().contains("history")) {
            // 调用聊天历史工具
            Object result = toolRegistry.invokeTool("chatHistory", new Object[]{"sample_session_id"});
            response = "Chat History: " + result.toString();
        } else {
            // 使用普通AI聊天
            response = ai.chat(content != null ? content : "");
        }
        
        return new BotReply(response);
    }
}