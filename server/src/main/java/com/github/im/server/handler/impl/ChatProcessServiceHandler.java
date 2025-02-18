package com.github.im.server.handler.impl;

import com.github.im.common.connect.connection.ReactiveConnectionManager;
import com.github.im.common.connect.connection.server.ProtoBufProcessHandler;
import com.github.im.common.connect.model.proto.BaseMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Hooks;
import reactor.netty.Connection;

/**
 * chat message process
 */
@Component
public class ChatProcessServiceHandler implements ProtoBufProcessHandler {

    @Override
    public BaseMessage.BaseMessagePkg.PayloadCase type() {

        return BaseMessage.BaseMessagePkg.PayloadCase.MESSAGE;
    }

    @Override
    public void process(Connection con, BaseMessage.BaseMessagePkg message) {

        Hooks.onOperatorDebug();

        // 1. 向 Sink 流中推送 Message 数据
        ReactiveConnectionManager.addChatMessage(message.getMessage());


    }
}
