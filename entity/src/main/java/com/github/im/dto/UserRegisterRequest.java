package com.github.im.dto;

import lombok.Data;


@Data
public class UserRegisterRequest {
    private String username;
    private String email;
    private String phoneNumber;
    private String password; // 用于接收用户输入的明文密码

}
