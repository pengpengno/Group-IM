package com.github.im.server.listener;

import com.github.im.server.event.MessageCreatedEvent;
import com.github.im.server.service.AutomationEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Keeps automation outside the message transaction and only sees committed messages. */
@Component
@RequiredArgsConstructor
public class AutomationMessageListener {
    private final AutomationEngine automationEngine;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageCreated(MessageCreatedEvent event) { automationEngine.onMessageCreated(event); }
}
