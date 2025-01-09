package com.github.im.server.handler;

import com.github.im.common.connect.connection.ConnectionConstants;
import com.github.im.common.connect.connection.ReactiveConnectionManager;
import com.github.im.common.connect.connection.server.ProtoBufProcessHandler;
import com.github.im.common.connect.connection.server.ServerToolkit;
import com.github.im.common.connect.connection.server.context.IConnectContextAction;
import com.github.im.common.connect.connection.server.context.ReactorConnection;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.common.connect.model.proto.Chat;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.Connection;

import java.util.Optional;

/**
 *  AccountInfo ProcessHandler
 */
@Component
@Slf4j
public class AccountInfoProcessHandler implements ProtoBufProcessHandler {

    private static final Sinks.Many<Connection> connectionsSink =
            Sinks.many().multicast().directAllOrNothing();

    @Override
    public BaseMessage.BaseMessagePkg.PayloadCase type() {
        return BaseMessage.BaseMessagePkg.PayloadCase.ACCOUNTINFO;
    }


    @Override
    public void process(@NotNull Connection con, BaseMessage.BaseMessagePkg message) {

        Hooks.onOperatorDebug();

        var accountInfo = message.getAccountInfo();

        Optional.ofNullable(con).ifPresent(connection -> {
            connection.channel().attr(ConnectionConstants.BING_ACCOUNT_KEY).set(accountInfo);

            // 过滤出符合条件的消息，并发送
            var chatMessages = ReactiveConnectionManager.getChatMessages();
            chatMessages
                    .filter(chatMessage -> accountInfo.getAccount().equals(chatMessage.getToAccountInfo().getAccount())
                            &&!con.isDisposed())  // 过滤条件
                    .doOnNext(chatMessage -> {
                        // 可以在此进行日志记录等操作
                        log.debug("Sending chat message to: {}", accountInfo.getAccount());
                    })
                    .flatMap(chatMessage -> {
                        // 返回消息流给 connection.outbound() 进行发送
                        Chat.ChatMessage.Builder builder = Chat.ChatMessage.newBuilder()
                                .setFromAccountInfo(chatMessage.getFromAccountInfo())
                                .setToAccountInfo(chatMessage.getToAccountInfo())
                                .setContent("111111111111111")
                                ;

                        var baseChatMessage = BaseMessage.BaseMessagePkg.newBuilder()
                                .setMessage(builder.build())
                                .build();
                        return connection.outbound().sendObject(Mono.just(baseChatMessage));
                    }).checkpoint()
                    .doOnTerminate(() -> {
                        // 可以在流终止时执行清理操作
                        log.debug("Chat message stream completed.");
                    })
                    .doOnError(ex -> {
                        log.error("Error occurred while sending chat message to: {}", accountInfo.getAccount(), ex);
                    })
                    .subscribe(
                            // 订阅并处理流
                            null,  // 这里可以传入一个处理成功的回调函数
                            error -> log.error("Error occurred while processing chat message stream")  // 错误处理
                    );
            // 监听连接关闭事件，当连接被关闭时取消订阅
            con.onDispose()
                .doOnTerminate(() -> {
                    // 连接关闭时，取消订阅，执行清理操作
                    log.debug("Connection closed, cancelling message stream subscription.");
                    chatMessages.subscribe().dispose();  // 显式取消流的订阅
                })
                .subscribe();

        });


    }
}
