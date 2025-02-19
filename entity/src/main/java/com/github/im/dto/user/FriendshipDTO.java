package com.github.im.dto.user;

import lombok.Builder;
import lombok.Data;

@Data
//@Builder
public class FriendshipDTO {

    private Long id;

    private UserInfo userInfo;

    private UserInfo friendUserInfo ;

}