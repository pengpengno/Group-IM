package com.github.im.server.ai;

import org.springframework.stereotype.Component;

/**
 * 提示词工厂
 */
@Component
public class PromptFactory {

    /**
     * 创建总结提示词
     */
    public String createSummaryPrompt(String content) {
        return "请总结以下内容：" + content;
    }

    /**
     * 创建通用对话提示词
     */
    public String createChatPrompt(String userInput) {
        return userInput;
    }

    /**
     * 创建特定领域提示词
     */
    public String createDomainPrompt(String domain, String userInput) {
        return "你是专业的" + domain + "助手，请回答：" + userInput;
    }
}