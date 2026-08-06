package com.github.im.server.config.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.UserMessage;
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

    @Value("${spring.ai.deepseek.api-key:#{null}}")
    private String deepSeekApiKey;

    @Value("${spring.ai.deepseek.base-url:https://api.deepseek.com}")
    private String deepSeekBaseUrl;

    @Value("${spring.ai.groq.api-key:#{null}}")
    private String groqApiKey;

    @Value("${spring.ai.groq.base-url:https://api.groq.com/openai/v1}")
    private String groqBaseUrl;

    @Value("${spring.ai.openai.api-key:#{null}}")
    private String openaiApiKey;

    @Value("${spring.ai.openai.base-url:https://api.openai.com/v1}")
    private String openaiBaseUrl;

    private boolean isDeepSeekConfigured() {
        return StringUtils.hasText(deepSeekApiKey) && !deepSeekApiKey.startsWith("your-");
    }

    private boolean isGroqConfigured() {
        return StringUtils.hasText(groqApiKey) && !groqApiKey.startsWith("your-");
    }

    private boolean isOpenAIConfigured() {
        return StringUtils.hasText(openaiApiKey) && !openaiApiKey.startsWith("your-");
    }

    public boolean isAiEnabled() {
        return isDeepSeekConfigured() || isGroqConfigured() || isOpenAIConfigured();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.ai.deepseek.api-key")
    public OpenAiApi deepSeekApi() {
        return new OpenAiApi(deepSeekBaseUrl, new SimpleApiKey(deepSeekApiKey), null, null, null, null, null, null);
    }

    @Bean("deepSeekChatClient")
    @ConditionalOnProperty(name = "spring.ai.deepseek.api-key")
    public ChatClient deepSeekChatClient(OpenAiApi deepSeekApi) {
        return ChatClient.builder(new OpenAiChatModel(deepSeekApi, null, null, null, null)).build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.ai.groq.api-key")
    public OpenAiApi groqApi() {
        return new OpenAiApi(groqBaseUrl, new SimpleApiKey(groqApiKey), null, null, null, null, null, null);
    }

    @Bean("groqChatClient")
    @ConditionalOnProperty(name = "spring.ai.groq.api-key")
    public ChatClient groqChatClient(OpenAiApi groqApi) {
        return ChatClient.builder(new OpenAiChatModel(groqApi, null, null, null, null)).build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.ai.openai.api-key")
    public OpenAiApi openAiApi() {
        return new OpenAiApi(openaiBaseUrl, new SimpleApiKey(openaiApiKey), null, null, null, null, null, null);
    }

    @Bean("openAiChatClient")
    @ConditionalOnProperty(name = "spring.ai.openai.api-key")
    public ChatClient openAiChatClient(OpenAiApi openAiApi) {
        return ChatClient.builder(new OpenAiChatModel(openAiApi, null, null, null, null)).build();
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.ai.enabled", havingValue = "true")
    public ChatClient defaultChatClient() {
        if (isDeepSeekConfigured()) {
            log.info("Using DeepSeek as the default AI provider");
            return buildClient(deepSeekBaseUrl, deepSeekApiKey);
        }
        if (isGroqConfigured()) {
            log.info("Using Groq as the default AI provider");
            return buildClient(groqBaseUrl, groqApiKey);
        }
        if (isOpenAIConfigured()) {
            log.info("Using OpenAI as the default AI provider");
            return buildClient(openaiBaseUrl, openaiApiKey);
        }
        throw new IllegalStateException("AI is enabled but no provider API key is configured");
    }

    private ChatClient buildClient(String baseUrl, String apiKey) {
        OpenAiApi api = new OpenAiApi(baseUrl, new SimpleApiKey(apiKey), null, null, null, null, null, null);
        return ChatClient.builder(new OpenAiChatModel(api, null, null, null, null)).build();
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.ai.enabled", havingValue = "false", matchIfMissing = true)
    public ChatClient disabledChatClient() {
        log.info("AI is disabled; creating a deterministic disabled ChatClient");
        return ChatClient.builder(chatModel -> {
            UserMessage userMessage = chatModel.getUserMessage();
            ChatClientResponse response = ChatClientResponse.builder()
                    .chatResponse(null)
                    .context(new HashMap<>())
                    .context("message", "AI功能未启用，无法处理请求")
                    .context("success", false)
                    .build();
            log.debug("Rejected AI request while disabled: {}", userMessage == null ? "" : userMessage.getText());
            return response.chatResponse();
        }).build();
    }
}
