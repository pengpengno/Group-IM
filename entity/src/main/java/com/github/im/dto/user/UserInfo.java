package com.github.im.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
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

    @JsonInclude(JsonInclude.Include.NON_NULL) // 不为null时才序列化
    private String avatar;

    private String email;

    @JsonInclude(JsonInclude.Include.NON_NULL) //不为null时才序列化
    private String token ;


    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String refreshToken;

}
