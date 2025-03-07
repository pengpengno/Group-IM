package com.github.im.conversation;

import com.github.im.dto.user.UserInfo;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;


@Data
public class GroupInfo {

    private String groupName;
    private String description;

    @NotEmpty()
    private List<UserInfo> members;

}