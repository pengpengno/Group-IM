package com.github.im.server.config.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import java.util.HashMap;

@Configuration
@Slf4j
public class MultiAiConfig {

    // Groq配置（使用OpenAI兼容接口）
    @Value("${spring.ai.groq.api-key:#{null}}")
    private String groqApiKey;

    @Value("${spring.ai.groq.base-url:https://api.groq.com/openai/v1}")
    private String groqBaseUrl;

    // OpenAI配置
    @Value("${spring.ai.openai.api-key:#{null}}")
    private String openaiApiKey;

    @Value("${spring.ai.openai.base-url:https://api.openai.com/v1}")
    private String openaiBaseUrl;

    // 检查是否配置了Groq API
    private boolean isGroqConfigured() {
        return StringUtils.hasText(groqApiKey) && !groqApiKey.equals("your-groq-default-key");
    }

    // 检查是否配置了OpenAI API
    private boolean isOpenAIConfigured() {
        return StringUtils.hasText(openaiApiKey) && !openaiApiKey.equals("your-default-key");
    }

    /**
     * 检查是否启用了AI功能
     */
    public boolean isAiEnabled() {
        return isGroqConfigured() || isOpenAIConfigured();
    }

    /**
     * Groq API配置（使用OpenAI兼容接口）
     */
    @Bean
    @ConditionalOnProperty(name = "spring.ai.groq.api-key", matchIfMissing = false)
    public OpenAiApi groqApi() {
        return new OpenAiApi(groqBaseUrl, new SimpleApiKey(groqApiKey), null, null, null, null, null, null);
    }

    /**
     * Groq聊天模型
     */
    @Bean
    @ConditionalOnProperty(name = "spring.ai.groq.api-key", matchIfMissing = false)
    public OpenAiChatModel groqChatModel(OpenAiApi groqApi) {
        return new OpenAiChatModel(groqApi, null, null, null, null);
    }

    /**
     * Groq ChatClient
     */
    @Bean("groqChatClient")
    @ConditionalOnProperty(name = "spring.ai.groq.api-key", matchIfMissing = false)
    public ChatClient groqChatClient(OpenAiChatModel groqChatModel) {
        return ChatClient.builder(groqChatModel).build();
    }

    /**
     * OpenAI API配置
     */
    @Bean
    @ConditionalOnProperty(name = "spring.ai.openai.api-key", matchIfMissing = false)
    public OpenAiApi openAiApi() {
        return new OpenAiApi(openaiBaseUrl, new SimpleApiKey(openaiApiKey), null, null, null, null, null, null);
    }

    /**
     * OpenAI聊天模型
     */
    @Bean
    @ConditionalOnProperty(name = "spring.ai.openai.api-key", matchIfMissing = false)
    public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi) {
        return new OpenAiChatModel(openAiApi, null, null, null, null);
    }

    /**
     * OpenAI ChatClient
     */
    @Bean("openAiChatClient")
    @ConditionalOnProperty(name = "spring.ai.openai.api-key", matchIfMissing = false)
    public ChatClient openAiChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel).build();
    }

    /**
     * 默认ChatClient（根据配置选择模型）
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.ai.enabled", havingValue = "true", matchIfMissing = false)
    public ChatClient defaultChatClient() {
        // 如果配置了Groq，返回Groq客户端
        if (isGroqConfigured()) {
            OpenAiApi api = new OpenAiApi(groqBaseUrl, new SimpleApiKey(groqApiKey), null, null, null, null, null, null);
            OpenAiChatModel model = new OpenAiChatModel(api, null, null, null, null);
            return ChatClient.builder(model).build();
        } 
        // 如果配置了OpenAI，返回OpenAI客户端
        else if (isOpenAIConfigured()) {
            OpenAiApi api = new OpenAiApi(openaiBaseUrl, new SimpleApiKey(openaiApiKey), null, null, null, null, null, null);
            OpenAiChatModel model = new OpenAiChatModel(api, null, null, null, null);
            return ChatClient.builder(model).build();
        } 
        // 如果都没有配置，抛出异常提醒用户
        else {
            log.warn("AI功能未启用：请配置有效的AI服务API密钥。");
            OpenAiApi api = new OpenAiApi(openaiBaseUrl, new SimpleApiKey("your-default-key"), null, null, null, null, null, null);
            OpenAiChatModel model = new OpenAiChatModel(api, null, null, null, null);

            return ChatClient.builder(model).build();
//            throw new IllegalStateException("AI功能未启用：需要配置至少一种AI服务的API密钥（Groq或OpenAI）。请设置相应的配置属性。");
        }


    }

    /**
     * 当AI未启用时的备用ChatClient
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.ai.enabled", havingValue = "false", matchIfMissing = true)
    public ChatClient disabledChatClient() {
        log.info("AI功能已被禁用，创建空的ChatClient实现");
        return ChatClient.builder((chatModel) -> {
            log.warn("AI功能当前不可用，请配置有效的API密钥");
            UserMessage userMessage = chatModel.getUserMessage();

            // 构造一个空的 ChatClientResponse
            ChatClientResponse response = ChatClientResponse.builder()
                    .chatResponse(null) // AI未启用，无真正响应
                    .context(new HashMap<>()) // 空上下文
                    .context("message", "AI功能未启用，无法处理请求")
                    .context("success", false)
                    .build();


            return response.chatResponse();
        }).build();
    }
}