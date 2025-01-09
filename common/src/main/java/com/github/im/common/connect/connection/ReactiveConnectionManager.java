package com.github.im.common.connect.connection;

import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.common.connect.model.proto.Chat;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.netty.Connection;


@Slf4j
public class ReactiveConnectionManager {
    private static final Sinks.Many<Connection> connectionsSink = Sinks.many().multicast().onBackpressureBuffer();
    private static final Sinks.Many<Chat.ChatMessage> chatMessageSink = Sinks.many().multicast().onBackpressureBuffer();

    public static void addConnection(Connection connection) {
        connectionsSink.tryEmitNext(connection).orThrow(); // 添加错误处理
        connection.onDispose(() -> {
            // Handle connection removal logic here
            log.info("Connection removed: {}", connection.channel().remoteAddress());
        });
    }


    public static void addChatMessage(Chat.ChatMessage chatMessage) {
        chatMessageSink.tryEmitNext(chatMessage).orThrow(); // 添加错误处理
    }

    public static Flux<Chat.ChatMessage> getChatMessages() {
        return chatMessageSink.asFlux();
    }

    public static Flux<Connection> getConnections() {
        return connectionsSink.asFlux();
    }
}
