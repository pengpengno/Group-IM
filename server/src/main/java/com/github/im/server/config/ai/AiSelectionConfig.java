package com.github.im.server.config.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiSelectionConfig {

    @Value("${app.ai.provider:openai}") // 默认使用OpenAI
    private String aiProvider;

    /**
     * 根据配置选择主要的ChatClient
     */
    @Bean("primaryChatClient")
//    @ConditionalOnProperty(name = "spring.ai.enabled", havingValue = "true")
    public ChatClient primaryChatClient(MultiAiConfig multiAiConfig) {
        if (!multiAiConfig.isAiEnabled()) {
            return  multiAiConfig.disabledChatClient();

//            throw new IllegalStateException("AI功能未启用：请先配置有效的AI服务API密钥。");
        }
        
        // 直接返回默认的ChatClient，因为MultiAiConfig已经处理了选择逻辑
        return multiAiConfig.defaultChatClient();
    }
}