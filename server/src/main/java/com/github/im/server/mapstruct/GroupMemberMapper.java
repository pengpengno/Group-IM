package com.github.im.server.mapstruct;

import com.github.im.dto.GroupMemberDTO;
import com.github.im.dto.user.UserInfo;
import com.github.im.server.model.GroupMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface GroupMemberMapper {
    GroupMemberMapper INSTANCE = Mappers.getMapper(GroupMemberMapper.class);

    GroupMemberDTO groupMemberToGroupMemberDTO(GroupMember groupMember);


    GroupMember groupMemberDTOToGroupMember(GroupMemberDTO groupMemberDTO);


    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.email", target = "email")
    UserInfo toUserInfo(GroupMember groupMember);

    List<UserInfo> toUserInfoList(List<GroupMember> members);


}
