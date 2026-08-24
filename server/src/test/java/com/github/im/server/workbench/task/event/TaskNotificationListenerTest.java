package com.github.im.server.workbench.task.event;

import com.github.im.server.service.notification.ClientEvent;
import com.github.im.server.service.notification.ClientEventPublisher;
import com.github.im.server.workbench.integration.notification.WorkbenchDeepLinkFactory;
import com.github.im.server.workbench.integration.notification.WorkbenchDeliveryChannel;
import com.github.im.server.workbench.integration.notification.WorkbenchRolloutGate;
import com.github.im.server.workbench.task.model.TaskActivityAction;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaskNotificationListenerTest {

    @Test
    void defaultOffGateEmitsNothing() {
        WorkbenchRolloutGate gate = mock(WorkbenchRolloutGate.class);
        ClientEventPublisher publisher = mock(ClientEventPublisher.class);
        TaskNotificationListener listener = new TaskNotificationListener(
                gate, new WorkbenchDeepLinkFactory(), publisher);

        listener.onTaskNotification(event());

        verify(publisher, never()).publishWorkbenchResourceEvent(any(), eq(false));
    }

    @Test
    void sharesStableEventIdAndHonorsIndependentPushGate() {
        WorkbenchRolloutGate gate = mock(WorkbenchRolloutGate.class);
        when(gate.allows(WorkbenchDeliveryChannel.CLIENT_EVENT)).thenReturn(true);
        when(gate.allows(WorkbenchDeliveryChannel.PUSH)).thenReturn(false);
        ClientEventPublisher publisher = mock(ClientEventPublisher.class);
        TaskNotificationListener listener = new TaskNotificationListener(
                gate, new WorkbenchDeepLinkFactory(), publisher);

        listener.onTaskNotification(event());

        verify(publisher).publishWorkbenchResourceEvent(
                org.mockito.ArgumentMatchers.argThat(value -> matches(value)), eq(false));
    }

    private boolean matches(ClientEvent event) {
        assertEquals("evt-1", event.getEventId());
        assertEquals(20L, event.getReceiverId());
        assertEquals("group://workbench/task/101?companyId=7", event.getDeepLink());
        assertEquals("ASSIGN", event.getExtra().get("action"));
        return true;
    }

    private TaskNotificationEvent event() {
        return new TaskNotificationEvent(
                "evt-1", 7L, 101L, "发布准备", "TODO",
                TaskActivityAction.ASSIGN, 10L, List.of(20L), null,
                Instant.parse("2026-08-24T10:00:00Z"));
    }
}
