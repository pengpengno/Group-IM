package com.github.im.server.ai;

/**
 * 机器人处理器接口
 */
public interface BotHandler {
    
    /**
     * 判断当前处理器是否能处理该消息
     */
    boolean canHandle(Message msg);

    /**
     * 处理消息并返回回复
     */
    BotReply handle(Message msg, BotContext context);
}