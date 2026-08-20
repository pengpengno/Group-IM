package com.github.im.dto.workbench.task;

public record CreateTaskCommentRequest(String content, Long replyToId) {
}
