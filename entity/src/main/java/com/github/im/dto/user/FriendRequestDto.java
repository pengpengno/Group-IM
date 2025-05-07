package com.github.im.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class FriendRequestDto implements Serializable {


    private Long userId;


    private Long friendId;


    private String account ;

    private String friendAccount;

    private String friendName;

    private String userName ;

    private String remark;

    private String applyRemark;

}
