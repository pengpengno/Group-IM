package com.github.im.dto.message;

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
    /**
     * 排序字段
     */
    private String sort;
    
    /**
     * 从指定的 sequenceId 开始拉取消息
     */
    private Long fromSequenceId;


    /***
     * 拉去截至到 的 sequenceId
     */
    private Long toSequenceId;

}