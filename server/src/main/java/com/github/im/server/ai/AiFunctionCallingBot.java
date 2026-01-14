package com.github.im.server.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * AI函数调用机器人 - 演示Spring AI工具调用功能
 */
@Component
public class AiFunctionCallingBot implements BotHandler {

    private final AiChatService ai;
    private final PromptFactory promptFactory;
    private final ObjectMapper objectMapper;

    @Autowired
    public AiFunctionCallingBot(AiChatService ai, PromptFactory promptFactory) {
        this.ai = ai;
        this.promptFactory = promptFactory;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean canHandle(Message msg) {
        String content = msg.getContent();
        if (content == null) return false;
        
        // 检查是否包含需要工具调用的关键词
        return content.toLowerCase().contains("user info") || 
               content.toLowerCase().contains("contact info") ||
               content.toLowerCase().contains("get user") ||
               content.contains("/ask ");
    }

    @Override
    public BotReply handle(Message msg, BotContext ctx) {
        String content = msg.getContent();
        
        // 如果是工具调用请求
        if (content.contains("/ask ")) {
            // 解析工具调用请求
            String[] parts = content.split(" ", 3);
            if (parts.length >= 3) {
                String toolName = parts[1];
                String params = parts[2];
                
                // 执行工具调用
                String result = ai.executeToolCall(toolName, params);
                return new BotReply(result);
            }
        }
        
        // 对于用户查询请求，使用AI进行意图识别并可能触发工具调用
        String response = ai.chat(content);
        
        // 在实际实现中，这里会使用更复杂的逻辑来决定是否调用工具
        // 例如：解析AI响应中的工具调用指令
        
        return new BotReply(response);
    }
}