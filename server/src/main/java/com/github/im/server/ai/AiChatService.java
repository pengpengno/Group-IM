package com.github.im.server.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Map;

/** Generic language-model dialogue only. Tool execution belongs to the MCP gateway. */
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final ChatClient chatClient;
    private final AiTraceService aiTraceService;

    public String chat(String prompt) {
        if (chatClient == null) return "AI 功能未启用：请配置有效的 AI 服务 API 密钥。";
        try {
            return chatClient.prompt(prompt).call().content();
        } catch (Exception exception) {
            return "AI 服务暂时不可用：" + exception.getMessage();
        }
    }

    public String chatWithContext(String prompt, Map<String, Object> context) {
        return chat(prompt);
    }

    /** Kept for API compatibility; it does not grant any direct tool access. */
    public String chatWithTools(String prompt) {
        return chat(prompt);
    }
}
