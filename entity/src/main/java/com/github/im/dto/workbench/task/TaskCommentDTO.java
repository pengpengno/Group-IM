package com.github.im.dto.workbench.task;

import java.time.LocalDateTime;

public record TaskCommentDTO(
        Long commentId,
        Long authorId,
        String content,
        Long replyToId,
        LocalDateTime createdAt
) {
}
