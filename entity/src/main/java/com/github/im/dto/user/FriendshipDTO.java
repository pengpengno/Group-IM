package com.github.im.dto.user;

import lombok.Data;

@Data
public class FriendshipDTO {
    private Long id;

    private UserInfo userInfo;

    private UserInfo friendUserInfo ;

}