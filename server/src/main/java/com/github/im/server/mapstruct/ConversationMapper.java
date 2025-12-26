package com.github.im.server.mapstruct;

import com.github.im.conversation.ConversationRes;
import com.github.im.server.model.Conversation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring",uses = {GroupMemberMapper.class,UserMapper.class})
public interface ConversationMapper {


    @Mapping(target ="createdAt", source = "createdAt")
    @Mapping(target ="createdBy", source = "createdBy")
    @Mapping(target ="createUserId", source = "createdBy.userId")
    ConversationRes toDTO(Conversation conversation);

}