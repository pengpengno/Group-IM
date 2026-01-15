package com.github.im.server.ai;

import com.github.im.dto.message.MessageDTO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 消息路由组件
 */
@Component("aiMessageRouter")
public class MessageRouter {

    private final List<BotHandler> handlers;

    public MessageRouter(List<BotHandler> handlers) {
        this.handlers = handlers;
    }

    public BotReply route(MessageDTO<?> msgDto) {
        Message msg = new Message(msgDto);
        return handlers.stream()
                .filter(h -> h.canHandle(msg))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No suitable bot handler found for message: " + msg.getContent()))
                .handle(msg, new BotContext());
    }
}