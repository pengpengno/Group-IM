package com.github.im.server.mapstruct;

import com.github.im.conversation.ConversationRes;
import com.github.im.server.model.Conversation;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring",uses = {GroupMemberMapper.class})
public interface ConversationMapper {


    ConversationRes toDTO(Conversation conversation);
}