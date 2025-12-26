package com.github.im.server.handler.impl;

import com.github.im.common.connect.connection.ReactiveConnectionManager;
import com.github.im.common.connect.connection.server.BindAttr;
import com.github.im.common.connect.connection.server.ProtoBufProcessHandler;
import com.github.im.common.connect.model.proto.Account;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.common.connect.model.proto.Chat;
import com.github.im.server.model.User;
import com.github.im.server.service.ConversationService;
import com.github.im.server.service.MessageService;
import com.github.im.server.service.CompanyUserService;
import com.github.im.server.service.CompanyService;
import com.github.im.server.util.SchemaSwitcher;
import com.github.im.server.utils.EnumsTransUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;
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
    private final CompanyUserService companyUserService;
    private final CompanyService companyService;

    @Override
    public BaseMessage.BaseMessagePkg.PayloadCase type() {
        return BaseMessage.BaseMessagePkg.PayloadCase.MESSAGE;
    }

    @Override
    public void process(Connection con, BaseMessage.BaseMessagePkg message) {
        final var chatMessage = message.getMessage();
        final var clientMsgId = chatMessage.getClientMsgId();
        if (clientMsgId.isEmpty()) {
            // 不存在直接返回  ，打印数据信息日志
            log.error("消息 {} 的 clientMsgId 为空 payload {}", chatMessage.getMsgId(), chatMessage);
            return;
        }

        log.info("接收到消息 fromAccountInfo {}", chatMessage.getFromAccountInfo());
        log.info("content: {}", chatMessage.getContent());
        log.info("clientMsgId: {}", chatMessage.getClientMsgId());
        log.info("conversationId: {}", chatMessage.getConversationId());

        var fromAccountInfo = chatMessage.getFromAccountInfo();
        var userId = fromAccountInfo.getUserId();
        var account = fromAccountInfo.getAccount();

        User user = con.channel().attr(AccountInfoProcessHandler.BING_USER_KEY).get();
        var schemaName = user.getCurrentSchema();

        // 使用响应式方式处理消息保存和推送
        SchemaSwitcher.executeInSchema(schemaName, () -> {
            // 保存消息
            var saveMessage = messageService.saveMessage(chatMessage);
            var sequenceId = saveMessage.getSequenceId();
            val msgId = saveMessage.getMsgId();
            // 复制一份 用于推送到各个客户端
            var epochMilli = saveMessage.getTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            final var newChatMessage = Chat.ChatMessage.newBuilder(chatMessage)
                    .setSequenceId(sequenceId)
                    .setFromAccountInfo(Account.AccountInfo.newBuilder(fromAccountInfo).setAccessToken("").build())
                    .setServerTimeStamp(epochMilli)
                    .setMsgId(msgId)
                    .setMessagesStatus(EnumsTransUtil.convertMessageStatus(saveMessage.getStatus()))
                    .build();

            final var newBaseMessage = BaseMessage.BaseMessagePkg.newBuilder(message)
                    .setMessage(newChatMessage)
                    .build();

            var conversationId = chatMessage.getConversationId();
            
            // 在同一个schema上下文中获取会话成员
            var membersByGroupId = conversationService.getMembersByGroupId(conversationId);

            Optional.ofNullable(membersByGroupId)
                .ifPresent(members -> {
                    members.parallelStream()
//                        .filter(e-> !Objects.equals(e.getUsername(), fromAccountInfo.getAccount()))
                        .forEach(member -> {
                            var bindAttr = BindAttr.getBindAttrForPush(member.getUsername());
                            ReactiveConnectionManager.addBaseMessage(bindAttr, newBaseMessage);
                        });
                });

            return saveMessage; // 返回值，但在这里我们不使用它
        });
    }

    /**
     * 根据用户ID获取对应的schema名称
     * 通过查询用户信息获取其所属公司的schema
     */
    private String getUserSchemaName(Long userId, String account) {
        // 获取用户所属的公司ID列表
        var companyIds = companyUserService.getCompanyIdsByUserId(userId);
        if (companyIds != null && !companyIds.isEmpty()) {
            // 如果用户属于多个公司，可以使用默认公司或当前活跃公司
            // 这里我们使用第一个公司作为示例，实际实现可能需要更复杂的逻辑
            Long companyId = companyIds.get(0);
            return companyService.getSchemaNameByCompanyId(companyId);
        } else {
            // 如果用户不属于任何公司，返回public schema
            log.warn("用户 {} 不属于任何公司，使用默认public schema", userId);
            return "public";
        }
    }
}