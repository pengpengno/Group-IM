package com.github.im.server.dto;

import lombok.Data;

@Data

public class UserInfo {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;

    public UserInfo(Long id, String username, String nickname, String avatar) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.avatar = avatar;
    }

}
