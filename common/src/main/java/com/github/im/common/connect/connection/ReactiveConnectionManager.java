package com.github.im.common.connect.connection;

import com.github.im.common.connect.model.proto.Account;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.common.connect.model.proto.Chat;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.netty.Connection;

import java.util.concurrent.ConcurrentMap;


@Slf4j
public class ReactiveConnectionManager {
//    private static final Sinks.Many<Connection> connectionsSink = Sinks.many().multicast().onBackpressureBuffer();

//    private static final Sinks.Many<Chat.ChatMessage> chatMessageSink = Sinks.many().multicast().onBackpressureBuffer();



    private static final ConcurrentMap<String,Sinks.Many<Chat.ChatMessage>> chatMessageSinks = new java.util.concurrent.ConcurrentHashMap<>();


//    public static void addConnection(Connection connection) {
//
//        connectionsSink.tryEmitNext(connection).orThrow(); // 添加错误处理
//        connection.onDispose(() -> {
//            // Handle connection removal logic here
//            log.info("Connection removed: {}", connection.channel().remoteAddress());
//        });
//    }


    public static void addChatMessage(Chat.ChatMessage chatMessage) {

        var toAccountInfo = chatMessage.getToAccountInfo();
        toAccountInfo.getAccount();

        chatMessageSinks.putIfAbsent(toAccountInfo.getAccount(), Sinks.many().multicast().onBackpressureBuffer());

        chatMessageSinks.get(toAccountInfo.getAccount()).tryEmitNext(chatMessage).orThrow();
    }

    /**
     * 获取指定用户的 聊天channel
     * @param accountInfo
     * @return
     */
    public static Flux<Chat.ChatMessage> getChatMessages(Account.AccountInfo accountInfo) {
        var account = accountInfo.getAccountName();

//        var account1 = accountInfo.getAccount();
        chatMessageSinks.putIfAbsent(account, Sinks.many().multicast().onBackpressureBuffer());
        return chatMessageSinks.get(account).asFlux();
    }


    /**
     * 取消订阅
     * @param accountInfo
     */
    public static void unSubscribe(Account.AccountInfo accountInfo) {
        var account = accountInfo.getAccountName();

        var chatMessageMany = chatMessageSinks.get(account);

        chatMessageSinks.remove(account);

    }



//    public static Flux<Connection> getConnections() {
//        return connectionsSink.asFlux();
//    }
}
