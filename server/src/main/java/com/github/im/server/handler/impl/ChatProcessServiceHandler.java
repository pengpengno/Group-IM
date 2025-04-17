package com.github.im.server.handler.impl;

import com.github.im.common.connect.connection.ReactiveConnectionManager;
import com.github.im.common.connect.connection.server.BindAttr;
import com.github.im.common.connect.connection.server.ProtoBufProcessHandler;
import com.github.im.common.connect.model.proto.Account;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.common.connect.model.proto.Chat;
import com.github.im.dto.user.UserInfo;
import com.github.im.server.service.ConversationService;
import com.github.im.server.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Hooks;
import reactor.netty.Connection;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * chat message process
 */
@Component
@RequiredArgsConstructor
public class ChatProcessServiceHandler implements ProtoBufProcessHandler {


    private final ConversationService conversationService;

    private final MessageService messageService;

    @Override
    public BaseMessage.BaseMessagePkg.PayloadCase type() {

        return BaseMessage.BaseMessagePkg.PayloadCase.MESSAGE;
    }

    @Override
    public void process(Connection con, BaseMessage.BaseMessagePkg message) {

        Hooks.onOperatorDebug();

        final var chatMessage = message.getMessage();


        // 先将信息 存储
        messageService.saveMessage(chatMessage);

        var conversationId = chatMessage.getConversationId();
        var membersByGroupId = conversationService.getMembersByGroupId(conversationId);

        Optional.ofNullable(membersByGroupId)
            .ifPresent(members -> {
                members.stream()
                        .filter(e-> !Objects.equals(e.getUsername(), chatMessage.getFromAccountInfo().getAccount()))
                        .forEach(member -> {
                    var bindAttr = BindAttr.getBindAttr(member.getUsername());

                    ReactiveConnectionManager.addBaseMessage(bindAttr, message);
                });
            });



    }
}
