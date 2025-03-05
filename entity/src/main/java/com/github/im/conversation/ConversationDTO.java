package com.github.im.conversation;

import com.github.im.dto.user.UserInfo;
import lombok.Data;

import java.util.List;

@Data
public class ConversationDTO {
    private Long conversationId;
    private String groupName;
    private String description;
    private List<UserInfo> members;
    private String status;
}