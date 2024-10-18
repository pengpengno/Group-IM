package com.github.im.dto.user;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;


@Data
@NoArgsConstructor
public class UserRegisterRequest implements Serializable {
    private String username;
    private String email;
    private String phoneNumber;
    private String password; // 用于接收用户输入的明文密码

}
