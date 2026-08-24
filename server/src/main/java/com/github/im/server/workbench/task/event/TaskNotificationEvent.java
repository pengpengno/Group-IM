package com.github.im.server.workbench.task.event;

import com.github.im.server.workbench.task.model.TaskActivityAction;

import java.time.Instant;
import java.util.List;

public record TaskNotificationEvent(
        String eventId, Long companyId, Long taskId, String title, String status,
        TaskActivityAction action, Long actorId, List<Long> receiverIds,
        Long conversationId, Instant occurredAt
) {
}
