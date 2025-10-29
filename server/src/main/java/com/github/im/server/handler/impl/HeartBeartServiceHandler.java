package com.github.im.server.handler.impl;

import com.github.im.common.connect.connection.ReactiveConnectionManager;
import com.github.im.common.connect.connection.server.BindAttr;
import com.github.im.common.connect.connection.server.ProtoBufProcessHandler;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.common.connect.model.proto.Chat;
import com.github.im.server.service.ConversationService;
import com.github.im.server.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Hooks;
import reactor.netty.Connection;

import java.util.Objects;
import java.util.Optional;

/**
 * Heartbeat message processor
 * Handles ping/pong messages in Reactor Netty environment
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HeartBeartServiceHandler implements ProtoBufProcessHandler {


    private final ConversationService conversationService;

    private final MessageService messageService;

    @Override
    public BaseMessage.BaseMessagePkg.PayloadCase type() {

        return BaseMessage.BaseMessagePkg.PayloadCase.HEARTBEAT;
    }

    @Override
    public void process(Connection con, BaseMessage.BaseMessagePkg message) {

        Hooks.onOperatorDebug();

        try {
            if (message.hasHeartbeat()) {
                BaseMessage.Heartbeat heartbeat = message.getHeartbeat();
                
                // If this is a ping from client, respond with pong
                if (heartbeat.getPing()) {
                    log.debug("Received PING from client, sending PONG");
                    
                    // Create pong response
                    BaseMessage.BaseMessagePkg pong = BaseMessage.BaseMessagePkg.newBuilder()
                            .setHeartbeat(BaseMessage.Heartbeat.newBuilder().setPing(false).build())
                            .build();
                    
                    // Send pong back to client
                    con.outbound().sendObject(pong).then().subscribe();
                } else {
                    // This is a pong response from client
                    log.debug("Received PONG from client");
                }
            }
        } catch (Exception e) {
            log.error("Error processing heartbeat message", e);
        }
    }
}
