package com.github.im.server.mapstruct;

import com.github.im.conversation.ConversationDTO;
import com.github.im.dto.user.UserInfo;
import com.github.im.server.model.Conversation;
import com.github.im.server.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ConversationMapper {

    ConversationMapper INSTANCE = Mappers.getMapper(ConversationMapper.class);

    // Map User entity to UserInfo DTO
//    @Mapping(source = "userId", target = "id")
//    @Mapping(source = "avatarUrl", target = "avatar")
    ConversationDTO toDTO (Conversation conversation);
//    List<UserInfo> usersToUserInfos(List<User> user);

    // Map UserInfo DTO to User entity
//    @Mapping(source = "id", target = "userId")
//    @Mapping(source = "avatar", target = "avatarUrl")
//    User userInfoToUser(UserInfo userInfo);
}
