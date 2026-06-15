package com.github.im.server.service.notification;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PushPayloadMapper {

    public Map<String, String> toDataPayload(ClientEvent event) {
        Map<String, String> payload = new LinkedHashMap<>();
        put(payload, "eventId", event.getEventId());
        put(payload, "eventType", event.getEventType() != null ? event.getEventType().name() : null);
        put(payload, "priority", event.getPriority() != null ? event.getPriority().name() : null);
        put(payload, "receiverId", event.getReceiverId());
        put(payload, "senderId", event.getSenderId());
        put(payload, "senderName", event.getSenderName());
        put(payload, "conversationId", event.getConversationId());
        put(payload, "messageId", event.getMessageId());
        put(payload, "sequenceId", event.getSequenceId());
        put(payload, "roomId", event.getRoomId());
        put(payload, "title", event.getTitle());
        put(payload, "body", event.getBody());
        put(payload, "preview", event.getPreview());
        put(payload, "deepLink", event.getDeepLink());
        put(payload, "collapseKey", event.getCollapseKey());
        put(payload, "badgeDelta", event.getBadgeDelta());
        put(payload, "ttlSeconds", event.getTtlSeconds());

        if (event.getExtra() != null) {
            event.getExtra().forEach((key, value) -> put(payload, key, value));
        }
        return payload;
    }

    private void put(Map<String, String> payload, String key, Object value) {
        if (value == null) {
            return;
        }
        payload.put(key, String.valueOf(value));
    }
}
