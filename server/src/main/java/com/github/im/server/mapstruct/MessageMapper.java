package com.github.im.server.mapstruct;

import com.github.im.conversation.ConversationRes;
import com.github.im.dto.session.MessageDTO;
import com.github.im.server.model.Conversation;
import com.github.im.server.model.Message;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring",uses = {GroupMemberMapper.class})
public interface MessageMapper {


    MessageDTO toDTO(Message message);
}