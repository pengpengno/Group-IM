package com.github.im.server.ai;

import org.springframework.stereotype.Component;

/**
 * AI摘要机器人 - 示例实现
 */
@Component
public class AiSummaryBot implements BotHandler {

    private final AiChatService ai;
    private final PromptFactory promptFactory;

    public AiSummaryBot(AiChatService ai, PromptFactory promptFactory) {
        this.ai = ai;
        this.promptFactory = promptFactory;
    }

    @Override
    public boolean canHandle(Message msg) {
        return msg.getContent() != null && msg.getContent().startsWith("/summary");
    }

    @Override
    public BotReply handle(Message msg, BotContext ctx) {
        String fullContent = msg.getContent();
        String contentToSummarize = fullContent.substring("/summary".length()).trim();
        String prompt = promptFactory.createSummaryPrompt(contentToSummarize);
        String response = ai.chat(prompt);
        return new BotReply(response);
    }
}