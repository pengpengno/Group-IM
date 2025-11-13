package com.github.im.server.handler.impl;

import com.github.im.common.connect.connection.ReactiveConnectionManager;
import com.github.im.common.connect.connection.server.BindAttr;
import com.github.im.common.connect.connection.server.ProtoBufProcessHandler;
import com.github.im.common.connect.model.proto.Account;
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

import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

/**
 * chat message process
 */
@Component
@RequiredArgsConstructor
@Slf4j
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
        final var clientMsgId = chatMessage.getClientMsgId();
        if (clientMsgId.isEmpty()) {
            // 不存在直接返回  ，打印数据信息日志
            log.error("消息 {} 的 clientMsgId 为空 payload {}", chatMessage.getMsgId(), chatMessage);
            return ;
        }

        var saveMessage = messageService.saveMessage(chatMessage);
        var sequenceId = saveMessage.getSequenceId();
        val msgId = saveMessage.getMsgId();
        // 复制一份 用于推送到各个客户端
        var epochMilli = saveMessage.getTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        final var newChatMessage = Chat.ChatMessage.newBuilder(chatMessage)
                .setSequenceId(sequenceId)
                .setServerTimeStamp(epochMilli)
                .setMsgId(msgId)
                .setMessagesStatus(Chat.MessagesStatus.SENT)
                .build();

        final var newBaseMessage = BaseMessage.BaseMessagePkg.newBuilder(message)
                .setMessage(newChatMessage)
                .build();

        var conversationId = chatMessage.getConversationId();
        var membersByGroupId = conversationService.getMembersByGroupId(conversationId);

        val fromAccountInfo = chatMessage.getFromAccountInfo();
        Optional.ofNullable(membersByGroupId)
            .ifPresent(members -> {
                members.parallelStream()
//                        .filter(e-> !Objects.equals(e.getUsername(), fromAccountInfo.getAccount()))
                        .forEach(member -> {

                    var bindAttr = BindAttr.getBindAttrForPush(member.getUsername());

                    ReactiveConnectionManager.addBaseMessage(bindAttr, newBaseMessage);

            });

        });


//        TODO  1.  QOS ACK 2. Encryption
            // 先实现 单向 ACK
//        Chat.AckMessage ackMessage = Chat.AckMessage.newBuilder()
//                .setClientMsgId(clientMsgId)
//                .setServerMsgId(msgId)
//                .setConversationId(conversationId)
//                .setFromAccount( Account.AccountInfo.newBuilder().setAccount(fromAccountInfo.getAccount()))
//                .setAckTimestamp(System.currentTimeMillis())
//                .setStatus(Chat.MessagesStatus.SENT)
//                .build();
//        BaseMessage.BaseMessagePkg ackMessagePkg = BaseMessage.BaseMessagePkg.newBuilder()
//                .setAck(ackMessage)
//                .build();
//
//        con.outbound().sendObject(ackMessagePkg).then().subscribe();
    }
}
