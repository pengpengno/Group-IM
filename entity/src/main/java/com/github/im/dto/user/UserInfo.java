package com.github.im.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo implements Serializable {
    private Long userId;

    private String username;

//    private String nickname;

    private String avatar;

    private String email;

    private String token ;

}
