package com.github.im.server.handler;

import com.github.im.common.connect.connection.ConnectionConstants;
import com.github.im.common.connect.connection.ReactiveConnectionManager;
import com.github.im.common.connect.connection.server.ProtoBufProcessHandler;
import com.github.im.common.connect.connection.server.ServerToolkit;
import com.github.im.common.connect.connection.server.context.IConnectContextAction;
import com.github.im.common.connect.enums.ProtocolMessageMapEnum;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.common.connect.model.proto.Chat;
import com.google.protobuf.Any;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Hooks;
import reactor.netty.Connection;

import java.util.Optional;

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

        ReactiveConnectionManager.addChatMessage(message.getMessage());


    }
}
