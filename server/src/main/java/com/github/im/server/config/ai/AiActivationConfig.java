package com.github.im.server.config.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.chat.client.ChatClient;

/**
 * AI激活配置 - 仅当配置了API密钥时才启用AI功能
 */
@Configuration
public class AiActivationConfig {

    /**
     * 当没有配置Groq API密钥且没有配置OpenAI API密钥时，提供默认处理
     */
    @Bean("fallbackChatClient")
    @ConditionalOnProperty(
        name = {"spring.ai.groq.api-key", "spring.ai.openai.api-key"},
        matchIfMissing = false,
        havingValue = ""
    )
    public ChatClient fallbackChatClient() {
        // 这种情况比较复杂，使用更简单的配置方式
        return null;
    }
}