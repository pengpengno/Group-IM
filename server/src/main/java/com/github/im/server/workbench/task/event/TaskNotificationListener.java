package com.github.im.server.workbench.task.event;

import com.github.im.dto.workbench.notification.WorkbenchCategory;
import com.github.im.dto.workbench.notification.WorkbenchProtocol;
import com.github.im.dto.workbench.notification.WorkbenchTarget;
import com.github.im.server.service.notification.ClientEvent;
import com.github.im.server.service.notification.ClientEventPriority;
import com.github.im.server.service.notification.ClientEventPublisher;
import com.github.im.server.service.notification.ClientEventType;
import com.github.im.server.workbench.integration.notification.WorkbenchDeepLinkFactory;
import com.github.im.server.workbench.integration.notification.WorkbenchDeliveryChannel;
import com.github.im.server.workbench.integration.notification.WorkbenchRolloutGate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TaskNotificationListener {
    private final WorkbenchRolloutGate rolloutGate;
    private final WorkbenchDeepLinkFactory deepLinkFactory;
    private final ClientEventPublisher clientEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskNotification(TaskNotificationEvent source) {
        if (!rolloutGate.allows(WorkbenchDeliveryChannel.CLIENT_EVENT)) return;

        String resourceId = String.valueOf(source.taskId());
        String deepLink = deepLinkFactory.create(
                new WorkbenchTarget(source.companyId(), WorkbenchCategory.TASK, resourceId));
        for (Long receiverId : source.receiverIds()) {
            ClientEvent event = ClientEvent.builder()
                    .eventId(source.eventId())
                    .eventType(ClientEventType.WORKBENCH_RESOURCE_EVENT)
                    .priority(ClientEventPriority.NORMAL)
                    .receiverId(receiverId)
                    .senderId(source.actorId())
                    .conversationId(source.conversationId())
                    .title(source.title())
                    .body(body(source))
                    .preview(source.title())
                    .deepLink(deepLink)
                    .collapseKey("task-" + source.taskId())
                    .badgeDelta(1)
                    .ttlSeconds(86400L)
                    .extra(Map.of(
                            "protocolVersion", WorkbenchProtocol.VERSION_1,
                            "notificationKind", "workbench_resource",
                            "category", WorkbenchCategory.TASK.name(),
                            "action", source.action().name(),
                            "resourceId", resourceId,
                            "companyId", source.companyId()
                    ))
                    .build();
            clientEventPublisher.publishWorkbenchResourceEvent(
                    event, rolloutGate.allows(WorkbenchDeliveryChannel.PUSH));
        }
    }

    private String body(TaskNotificationEvent event) {
        return switch (event.action()) {
            case ASSIGN -> "你被分配了任务";
            case COMPLETE -> "任务已完成";
            case REOPEN -> "任务已重新打开";
            default -> "任务已更新";
        };
    }
}
