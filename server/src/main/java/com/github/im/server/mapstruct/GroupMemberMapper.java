package com.github.im.server.mapstruct;

import com.github.im.dto.GroupMemberDTO;
import com.github.im.server.model.GroupMember;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface GroupMemberMapper {
    GroupMemberMapper INSTANCE = Mappers.getMapper(GroupMemberMapper.class);

    GroupMemberDTO groupMemberToGroupMemberDTO(GroupMember groupMember);


    GroupMember groupMemberDTOToGroupMember(GroupMemberDTO groupMemberDTO);
}
