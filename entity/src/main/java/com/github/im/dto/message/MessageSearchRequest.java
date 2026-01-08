package com.github.im.dto.message;

import lombok.Data;

@Data
public class MessageSearchRequest {
    private String keyword;  // 搜索关键字
    private Long sessionId;  // 会话ID
}