package com.github.im.server.mapstruct;

import com.github.im.dto.user.UserInfo;
import com.github.im.server.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    // Map User entity to UserInfo DTO
//    @Mapping(source = "userId", target = "id")
//    @Mapping(source = "avatarUrl", target = "avatar")
    UserInfo userToUserInfo(User user);

    // Map UserInfo DTO to User entity
//    @Mapping(source = "id", target = "userId")
//    @Mapping(source = "avatar", target = "avatarUrl")
    User userInfoToUser(UserInfo userInfo);
}
