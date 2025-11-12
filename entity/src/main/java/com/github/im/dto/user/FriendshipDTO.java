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

    /**
     * 状态 枚举
     * 1. pending  等待接受
     * 2. active  已接受
     * 3. rejected  被拒绝
     * 4. discord  单向舍弃
     *
     */
    private String status;

    private Long conversationId;

}