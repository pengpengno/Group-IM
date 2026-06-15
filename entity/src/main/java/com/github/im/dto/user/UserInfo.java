package com.github.im.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfo extends UserBasicInfo implements Serializable {

//
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String token ;  // token


    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String refreshToken;

    /**
     * 当前 登录的公司 Id
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long currentLoginCompanyId;

}
