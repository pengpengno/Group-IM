package com.github.im.server.mapstruct;

import com.github.im.conversation.ConversationRes;
import com.github.im.server.model.Conversation;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ConversationMapper {

    ConversationMapper INSTANCE = Mappers.getMapper(ConversationMapper.class);

    ConversationRes toDTO(Conversation conversation);
}