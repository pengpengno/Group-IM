package com.github.im.server.event;

/** Published only after MessageService has committed a normal chat message. */
public record MessageCreatedEvent(Long messageId, Long conversationId, Long senderId, String senderUsername, String content) { }
