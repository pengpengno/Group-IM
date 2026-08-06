package com.github.im.server.ai;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * AI摘要机器人
 */
@Component
@Order(10)
public class AiSummaryBot implements BotHandler {

    private final AiChatService ai;
    private final PromptFactory promptFactory;

    public AiSummaryBot(AiChatService ai, PromptFactory promptFactory) {
        this.ai = ai;
        this.promptFactory = promptFactory;
    }

    @Override
    public boolean canHandle(Message msg) {
        return msg.getContent() != null && msg.getContent().trim().startsWith("/summary");
    }

    @Override
    public BotReply handle(Message msg, BotContext ctx) {
        String fullContent = msg.getContent().trim();
        String contentToSummarize = fullContent.substring("/summary".length()).trim();
        if (contentToSummarize.isBlank()) {
            return new BotReply("请在 /summary 后提供需要总结的内容。当前会话历史摘要将在服务端消息闭环完成后接入。");
        }
        String prompt = promptFactory.createSummaryPrompt(contentToSummarize);
        return new BotReply(ai.chat(prompt), "markdown");
    }
}
