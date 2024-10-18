package com.github.meeting.common.model;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;

/***
 * 账户基本信息  公用模块
 */
@Getter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountInfo implements Serializable, AccountSPI {

    @NotNull(message = "userId could not be null ")
    private Long userId;  // 用户id
    @NotNull(message = "account  not null")
    private String account;  // 账户

    private String userName; // 用户姓名

//    @Email(message = "非法的邮箱格式！",groups = AccountAdd.class)
    private String email;  // 邮箱


    @Override
    public String account() {
        return getAccount();
    }

    @Override
    public Long userId() {
        return getUserId();
    }

    @Override
    public String accountName() {
        return userName;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}
