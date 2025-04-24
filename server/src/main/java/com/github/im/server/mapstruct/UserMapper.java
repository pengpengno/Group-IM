package com.github.im.server.mapstruct;

import com.github.im.dto.user.UserInfo;
import com.github.im.server.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);


    UserInfo userToUserInfo(User user);
    List<UserInfo> usersToUserInfos(List<User> user);

    // Map UserInfo DTO to User entity
//    @Mapping(source = "id", target = "userId")
//    @Mapping(source = "avatar", target = "avatarUrl")
    User userInfoToUser(UserInfo userInfo);
}
