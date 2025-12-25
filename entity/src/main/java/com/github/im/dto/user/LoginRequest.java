package com.github.im.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest implements Serializable {
    @NotBlank(message = "登录账号不能为空")
    private String loginAccount ;

    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 长期Token
     * 1. 刷新 短期 token
     * 2. 自动登录
     */
    private String  refreshToken;
    
//    /**
//     * 登录公司ID
//     */
//    private Long companyId;
    /**
     * 登录的公司的 Code
     * 指代公司的  schema_name
     */
    private String companyCode ;

}