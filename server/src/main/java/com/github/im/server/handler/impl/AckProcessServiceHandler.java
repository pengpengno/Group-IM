package com.github.im.server.handler.impl;

import com.github.im.common.connect.connection.server.ProtoBufProcessHandler;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.common.connect.model.proto.Chat;
import com.github.im.server.model.User;
import com.github.im.server.service.MessageService;
import com.github.im.server.util.SchemaSwitcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.netty.Connection;

/**
 * ACK 消息处理器
 * 处理客户端发送的 ACK 消息，如已读回执
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AckProcessServiceHandler implements ProtoBufProcessHandler {

    private final MessageService messageService;

    @Override
    public BaseMessage.BaseMessagePkg.PayloadCase type() {
        return BaseMessage.BaseMessagePkg.PayloadCase.ACK;
    }

    @Override
    public void process(Connection con, BaseMessage.BaseMessagePkg message) {
        final var ackMessage = message.getAck();
        final var status = ackMessage.getStatus();
        
        log.info("接收到 ACK 消息 fromUser: {}, conversationId: {}, status: {}", 
                ackMessage.getFromUser().getUsername(), 
                ackMessage.getConversationId(), 
                status);

        if (status == Chat.MessagesStatus.READ) {
            User user = con.channel().attr(UserInfoProcessHandler.BING_USER_KEY).get();
            if (user != null) {
                var schemaName = user.getCurrentSchema();
                SchemaSwitcher.executeInSchema(schemaName, () -> {
                    // 目前使用 max sequenceID 策略，ACK 中若包含 serverMsgId 可作为参考，但通常 READ 是按会话维度
                    // 假设客户端在 ACK 中带入了它看到的最大的 serverMsgId 或 sequenceId
                    // 这里我们暂时根据 AckMessage 中的 serverMsgId 关联的消息 sequenceId 来批量标记
                    // 注意：协议定义中 AckMessage 可能需要更多的上下文
                    messageService.markConversationAsRead(
                            ackMessage.getConversationId(), 
                            user.getUserId(), 
                            ackMessage.getServerMsgId() // 这里复用为 sequenceId 或 msgId，视前端实现而定
                    );
                });
            }
        }
    }
}
