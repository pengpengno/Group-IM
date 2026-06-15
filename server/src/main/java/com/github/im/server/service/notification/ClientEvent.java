package com.github.im.server.service.notification;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder
public class ClientEvent {
    String eventId;
    ClientEventType eventType;
    ClientEventPriority priority;
    Long receiverId;
    Long senderId;
    String senderName;
    Long conversationId;
    Long messageId;
    Long sequenceId;
    String roomId;
    String title;
    String body;
    String preview;
    String deepLink;
    String collapseKey;
    Integer badgeDelta;
    Long ttlSeconds;
    Map<String, Object> extra;
}
