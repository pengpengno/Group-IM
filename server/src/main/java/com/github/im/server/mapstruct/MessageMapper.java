package com.github.im.server.mapstruct;

import com.github.im.conversation.ConversationRes;
import com.github.im.dto.session.MessageDTO;
import com.github.im.dto.session.MessagePayLoad;
import com.github.im.server.model.Conversation;
import com.github.im.server.model.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",uses = {GroupMemberMapper.class, UserMapper.class})
public interface MessageMapper {


    @Mapping(source = "conversation.conversationId", target = "conversationId")
    @Mapping(source = "fromAccountId.userId", target = "fromAccountId")
    @Mapping(source = "fromAccountId.username", target = "fromAccount.username")
    @Mapping(target = "fromAccountId.refreshToken",ignore = true)
    @Mapping(target = "fromAccountId.token",ignore = true)
    @Mapping(source = "fromAccountId.userId", target = "fromAccount.userId")
    @Mapping(target = "payload" , ignore = true)
    MessageDTO<MessagePayLoad> toDTO(Message message);



    @Mapping(source = "conversationId", target = "conversation.conversationId")
    @Mapping(source = "fromAccountId", target = "fromAccountId.userId")
    Message toEntity(MessageDTO<MessagePayLoad> messageDTO);
}