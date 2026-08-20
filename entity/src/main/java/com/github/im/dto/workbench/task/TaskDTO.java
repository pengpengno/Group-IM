package com.github.im.dto.workbench.task;

import java.time.LocalDateTime;
import java.util.List;

public record TaskDTO(
        Long taskId,
        String title,
        String description,
        String status,
        String priority,
        Long creatorId,
        Long ownerId,
        Long departmentId,
        Long conversationId,
        LocalDateTime startAt,
        LocalDateTime dueAt,
        LocalDateTime completedAt,
        int progress,
        long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<TaskAssigneeDTO> assignees,
        List<TaskCommentDTO> comments
) {
    public TaskDTO {
        assignees = List.copyOf(assignees);
        comments = List.copyOf(comments);
    }
}
