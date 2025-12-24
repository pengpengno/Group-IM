package com.github.im.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 *  用户基础信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBasicInfo implements Serializable {

    private Long userId;

    private String username;

    private String email;  // 邮箱

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String phoneNumber;



}
