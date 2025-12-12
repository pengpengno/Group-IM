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
    @NotBlank
    private String loginAccount ;

    @NotBlank
    private String password;

    /**
     * 长期Token
     * 1. 刷新 短期 token
     * 2. 自动登录
     */
    private String  refreshToken;
    
    /**
     * 登录公司ID
     */
    private Long companyId;

}