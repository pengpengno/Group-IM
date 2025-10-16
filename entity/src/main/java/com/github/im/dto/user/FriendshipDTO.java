package com.github.im.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendshipDTO {

    private Long id;

    private UserInfo userInfo;

    private UserInfo friendUserInfo ;

    private Long conversationId;

}