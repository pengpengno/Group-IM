package com.github.im.common.connect.connection;

import com.github.im.common.connect.connection.server.BindAttr;
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

    private static final ConcurrentMap<String, Sinks.Many<Chat.ChatMessage>> chatMessageSinks = new java.util.concurrent.ConcurrentHashMap<>();

    private static final ConcurrentMap<BindAttr<String>, Sinks.Many<BaseMessage>> BASE_MESSAGE_SINKS
            = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 注册一个Sink流程用于处理特定属性的消息
     * 此方法旨在确保每个属性都有一个对应的Sink实例来处理消息，
     * 如果该属性尚未关联任何Sink，则创建一个新的Sink实例并关联
     *
     * @param ATTR 属性名称，用作在BASE_MESSAGE_SINKS映射中的键
     */
    public static Sinks.Many<BaseMessage> registerSinkFlow(BindAttr<String> ATTR) {
        // 使用putIfAbsent方法来确保在BASE_MESSAGE_SINKS映射中不存在该属性时，
        // 创建一个新的Sink实例并放入映射中，这避免了重复创建Sink实例

        var containsKey = BASE_MESSAGE_SINKS.containsKey(ATTR);

        if (containsKey) {
            return BASE_MESSAGE_SINKS.get(ATTR);
        }

        Sinks.Many<BaseMessage> sinkFlow = Sinks.many().multicast().onBackpressureBuffer();
        BASE_MESSAGE_SINKS.putIfAbsent(ATTR, sinkFlow);
        return sinkFlow;
    }

    /**
     * 获取已经注册的Sink流程
     * 此方法用于获取已经注册的Sink实例
     *
     * @param ATTR 属性名称，用作在BASE_MESSAGE_SINKS映射中的键
     * @return 已注册的Sink实例，如果不存在则返回null
     */
    public static Sinks.Many<BaseMessage> getSinkFlow(BindAttr<String> ATTR) {
        return BASE_MESSAGE_SINKS.get(ATTR);
    }

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
}
