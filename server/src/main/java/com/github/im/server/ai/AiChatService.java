package com.github.im.server.ai;

import com.github.im.server.ai.tool.UserInfoTool;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import java.util.Map;

/**
 * AI聊天服务
 */
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final ChatClient chatClient;
    private final ToolRegistry toolRegistry;
    private final AiTraceService aiTraceService;
    private final UserInfoTool userInfoTool;


    public String chat(String prompt) {
        if (chatClient == null) {
            return "AI功能未启用：请配置有效的AI服务API密钥。";
        }
        try {
            return chatClient.prompt(prompt).call().content();
        } catch (Exception e) {
            // 如果AI服务配置无效或API密钥不正确，返回友好提示
            return "AI服务暂时不可用：" + e.getMessage();
        }
    }
    
    /**
     * 带上下文的聊天
     */
    public String chatWithContext(String prompt, Map<String, Object> context) {
        if (chatClient == null) {
            return "AI功能未启用：请配置有效的AI服务API密钥。";
        }
        try {
            return chatClient.prompt(prompt).call().content();
        } catch (Exception e) {
            return "AI服务暂时不可用：" + e.getMessage();
        }
    }
    
    /**
     * 使用工具调用的聊天
     */
    public String chatWithTools(String prompt) {
        if (chatClient == null) {
            return "AI功能未启用：请配置有效的AI服务API密钥。";
        }
        try {
            return chatClient.prompt(prompt).call().content();
        } catch (Exception e) {
            return "AI服务暂时不可用：" + e.getMessage();
        }
    }
    
    /**
     * 执行工具调用
     */
    public String executeToolCall(String toolName, String... params) {
        switch (toolName.toLowerCase()) {
            case "get_user_info":
                if (params.length > 0) {
                    return userInfoTool.getUserInfo(params[0]);
                }
                break;
            case "get_user_contact":
                if (params.length > 0) {
                    return userInfoTool.getUserContactInfo(params[0]);
                }
                break;
            default:
                return "Unknown tool: " + toolName;
        }
        return "Invalid parameters for tool: " + toolName;
    }
}