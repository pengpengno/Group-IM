package com.github.im.group.gui.connect.handler;

import com.github.im.common.connect.connection.server.ProtoBufProcessHandler;
import com.github.im.common.connect.model.proto.BaseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Hooks;
import reactor.netty.Connection;

/**
 *client side  chat message process
 */
@Component
@Slf4j
public class ClientChatProcessServiceHandler implements ProtoBufProcessHandler {

    @Override
    public BaseMessage.BaseMessagePkg.PayloadCase type() {

        return BaseMessage.BaseMessagePkg.PayloadCase.MESSAGE;
    }

    @Override
    public void process(Connection con, BaseMessage.BaseMessagePkg message) {

        Hooks.onOperatorDebug();
        var chatMessage = message.getMessage();


        log.info("ChatProcessServiceHandler{}",chatMessage);

    }
}
