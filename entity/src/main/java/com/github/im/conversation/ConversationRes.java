package com.github.im.conversation;

import com.github.im.dto.user.UserInfo;
import com.github.im.enums.ConversationType;
import lombok.Data;

import java.util.List;

@Data

public class ConversationRes {

    private Long conversationId;
    private String groupName;
    private String description;
    private List<UserInfo> members;

    private Long createBy;

    private String createAt;

    /**
     * {@link com.github.im.enums.ConversationStatus}
     */
    private String status;


    /**
     * {@link ConversationType}
     */
//    private ConversationType conversationType;
    private String conversationType;


}