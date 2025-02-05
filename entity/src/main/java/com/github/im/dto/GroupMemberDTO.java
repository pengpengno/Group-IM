package com.github.im.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GroupMemberDTO {
    private Long groupId;

    private String groupName;

    private Long userId;

    private LocalDateTime joinedAt;

}
