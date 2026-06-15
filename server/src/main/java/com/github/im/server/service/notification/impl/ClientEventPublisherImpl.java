package com.github.im.server.service.notification.impl;

import com.github.im.server.model.Meeting;
import com.github.im.server.model.Message;
import com.github.im.server.model.User;
import com.github.im.server.service.notification.ClientEvent;
import com.github.im.server.service.notification.ClientEventPriority;
import com.github.im.server.service.notification.ClientEventPublisher;
import com.github.im.server.service.notification.ClientEventType;
import com.github.im.server.service.notification.NotificationPolicyDecision;
import com.github.im.server.service.notification.NotificationPolicyService;
import com.github.im.server.service.notification.PushNotificationGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientEventPublisherImpl implements ClientEventPublisher {

    private final NotificationPolicyService notificationPolicyService;
    private final PushNotificationGateway pushNotificationGateway;

    @Override
    public void publishChatMessageCreated(Message message, User sender, List<User> recipients) {
        if (message == null || sender == null || recipients == null || recipients.isEmpty()) {
            return;
        }

        for (User recipient : recipients) {
            if (recipient == null || recipient.getUserId().equals(sender.getUserId())) {
                continue;
            }

            ClientEvent event = ClientEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(ClientEventType.CHAT_MESSAGE_CREATED)
                    .priority(ClientEventPriority.NORMAL)
                    .receiverId(recipient.getUserId())
                    .senderId(sender.getUserId())
                    .senderName(sender.getUsername())
                    .conversationId(message.getConversation().getConversationId())
                    .messageId(message.getMsgId())
                    .sequenceId(message.getSequenceId())
                    .title(sender.getUsername())
                    .body(buildPreview(message.getContent()))
                    .preview(buildPreview(message.getContent()))
                    .deepLink("group://chat/" + message.getConversation().getConversationId())
                    .collapseKey("chat-" + message.getConversation().getConversationId())
                    .badgeDelta(1)
                    .ttlSeconds(86400L)
                    .extra(Map.of(
                            "notificationKind", "chat_message",
                            "clickAction", "open_chat",
                            "messageType", message.getType().name(),
                            "clientMsgId", message.getClientMsgId()
                    ))
                    .build();
            dispatch(event);
        }
    }

    @Override
    public void publishMeetingInviteCreated(Meeting meeting, User host, List<User> recipients) {
        if (meeting == null || host == null || recipients == null || recipients.isEmpty()) {
            return;
        }

        for (User recipient : recipients) {
            if (recipient == null || recipient.getUserId().equals(host.getUserId())) {
                continue;
            }

            ClientEvent event = ClientEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .eventType(ClientEventType.MEETING_INVITE_CREATED)
                    .priority(ClientEventPriority.HIGH)
                    .receiverId(recipient.getUserId())
                    .senderId(host.getUserId())
                    .senderName(host.getUsername())
                    .conversationId(meeting.getConversation().getConversationId())
                    .roomId(meeting.getRoomId())
                    .title(host.getUsername())
                    .body(buildMeetingInviteBody(host.getUsername(), meeting.getTitle()))
                    .preview(meeting.getTitle())
                    .deepLink("group://meeting/" + meeting.getRoomId())
                    .collapseKey("meeting-" + meeting.getRoomId())
                    .badgeDelta(1)
                    .ttlSeconds(120L)
                    .extra(Map.of(
                            "notificationKind", "meeting_invite",
                            "clickAction", "open_meeting",
                            "actionKind", "invite",
                            "meetingTitle", meeting.getTitle(),
                            "hostId", host.getUserId()
                    ))
                    .build();
            dispatch(event);
        }
    }

    private void dispatch(ClientEvent event) {
        NotificationPolicyDecision decision = notificationPolicyService.decide(event);
        log.info(
                "ClientEvent dispatched eventType={}, receiverId={}, presenceState={}, realtimeEnabled={}, pushEnabled={}, reason={}",
                event.getEventType(),
                event.getReceiverId(),
                decision.getPresenceState(),
                decision.isRealtimeEnabled(),
                decision.isPushEnabled(),
                decision.getReason()
        );

        if (decision.isPushEnabled()) {
            pushNotificationGateway.send(event);
        }
    }

    private String buildPreview(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        return content.length() <= 80 ? content : content.substring(0, 80);
    }

    private String buildMeetingInviteBody(String hostName, String meetingTitle) {
        String safeHost = hostName == null || hostName.isBlank() ? "Someone" : hostName;
        String safeTitle = meetingTitle == null || meetingTitle.isBlank() ? "meeting" : meetingTitle;
        return safeHost + " invited you to " + safeTitle;
    }
}
