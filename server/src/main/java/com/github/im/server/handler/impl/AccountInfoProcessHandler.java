package com.github.im.server.handler.impl;

import com.github.im.common.connect.connection.ConnectionConstants;
import com.github.im.common.connect.connection.ReactiveConnectionManager;
import com.github.im.common.connect.connection.server.BindAttr;
import com.github.im.common.connect.connection.server.ProtoBufProcessHandler;
import com.github.im.common.connect.model.proto.BaseMessage;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;

import java.util.Optional;

/**
 *  AccountInfo ProcessHandler
 */
@Component
@Slf4j
public class AccountInfoProcessHandler implements ProtoBufProcessHandler {



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
//            var chatMessages = ReactiveConnectionManager.getChatMessages(accountInfo);
            // 订阅 信息流
            var account = accountInfo.getAccount();
            var bindAttr = BindAttr.getBindAttr(accountInfo);

            var baseMessageMany = ReactiveConnectionManager.registerSinkFlow(bindAttr).asFlux();
            baseMessageMany
                .doOnNext(baseMessage -> {
                    // 可以在此进行日志记录等操作
                    log.debug("Sending chat message to: {}", account);
                })
                .flatMap(baseMessagePkg -> {
//                        // 返回消息流给 connection.outbound() 进行发送
                    return connection.outbound().sendByteArray(Mono.just(baseMessagePkg.toByteArray()));
                }).checkpoint()
                .doOnTerminate(() -> {
                    // 可以在流终止时执行清理操作
                    log.debug("Chat message stream completed.");
                })
                .doOnError(ex -> {
                    log.error("Error occurred while sending chat message to: {}", account, ex);
                })
                .subscribe(
                        // 订阅并处理流
                        null,  // 这里可以传入一个处理成功的回调函数
                        error -> log.error("Error occurred while processing chat message stream")  // 错误处理
                );

            // 监听连接关闭事件，当连接被关闭时取消订阅
            con.onDispose()
                .doOnTerminate(() -> {
//                    ReactiveConnectionManager.unSubscribe(accountInfo);
                    ReactiveConnectionManager.unSubscribe(bindAttr);
                    // 连接关闭时，取消订阅，执行清理操作
                    log.debug("Connection closed, cancelling message stream subscription.");
//                    chatMessages.subscribe().dispose();  // 显式取消流的订阅
                    baseMessageMany.subscribe().dispose();
                })
                .subscribe();

        });


    }
}
