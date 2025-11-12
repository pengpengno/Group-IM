package com.github.im.server.mapstruct;

import com.github.im.dto.user.FriendshipDTO;
import com.github.im.dto.user.UserInfo;
import com.github.im.server.model.Friendship;
import com.github.im.server.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring",uses = {UserMapper.class})
public interface FriendShipMapper {

    FriendShipMapper INSTANCE = Mappers.getMapper(FriendShipMapper.class);


    @Mapping(source = "user", target = "userInfo")
    @Mapping(source = "friend", target = "friendUserInfo")
    FriendshipDTO friendshipToFriendshipDTO(Friendship friendship);

    @Mapping(source = "userInfo", target = "user")
    @Mapping(source = "friendUserInfo", target = "friend")
    Friendship friendshipDTOToFriendship(FriendshipDTO friendshipDTO);

    // Add a method to convert a list of Friendship entities to a list of FriendshipDTOs
    List<FriendshipDTO> friendshipsToFriendshipDTOs(List<Friendship> friendships);

}
