package com.github.im.dto.session;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class MessagePullRequest {
    private Long conversationId;
    private Long fromAccountId;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int page = 0;
    private int size = 50;
    private String sort; // 可选排序字段（例如 id, createTime 等）

    // Getter & Setter
}
