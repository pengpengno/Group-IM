package com.github.im.server.mapstruct;

import com.github.im.dto.user.UserBasicInfo;
import com.github.im.dto.user.UserInfo;
import com.github.im.server.model.User;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    @Named("userInfo")
    @Mapping(target = "currentLoginCompanyId", source = "primaryCompanyId")
    UserInfo userToUserInfo(User user);

    @Named("basicUserInfo")
    UserBasicInfo userToUserBasicInfo(User user);
    @IterableMapping(qualifiedByName = "userInfo")
    List<UserInfo> usersToUserInfos(List<User> user);
    @IterableMapping(qualifiedByName = "basicUserInfo")
    List<UserBasicInfo> usersToUserBasicInfos(List<User> user);

    User userInfoToUser(UserInfo userInfo);
}
