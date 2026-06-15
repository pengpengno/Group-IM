package com.github.im.server.service.notification.impl;

import com.github.im.server.model.UserPrivacySetting;
import com.github.im.server.repository.UserPrivacySettingRepository;
import com.github.im.server.service.OnlineService;
import com.github.im.server.service.notification.ClientEvent;
import com.github.im.server.service.notification.ClientEventPriority;
import com.github.im.server.service.notification.ClientEventType;
import com.github.im.server.service.notification.ClientPresenceState;
import com.github.im.server.service.notification.NotificationPolicyDecision;
import com.github.im.server.service.notification.NotificationPolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultNotificationPolicyService implements NotificationPolicyService {

    private final OnlineService onlineService;
    private final UserPrivacySettingRepository userPrivacySettingRepository;

    @Override
    public NotificationPolicyDecision decide(ClientEvent event) {
        UserPrivacySetting privacySetting = event.getReceiverId() == null
                ? null
                : userPrivacySettingRepository.findByUserUserId(event.getReceiverId());

        String preference = privacySetting != null && privacySetting.getNotificationPreference() != null
                ? privacySetting.getNotificationPreference().trim().toLowerCase()
                : "all";

        ClientPresenceState presenceState = resolvePresence(event.getReceiverId());

        boolean realtimeEnabled = presenceState != ClientPresenceState.OFFLINE;
        boolean pushEnabled = presenceState == ClientPresenceState.OFFLINE;
        String reason = "default";

        boolean highPriorityMeetingInvite = event.getEventType() == ClientEventType.MEETING_INVITE_CREATED
                && event.getPriority() == ClientEventPriority.HIGH;

        if ("none".equals(preference) || "mute".equals(preference)) {
            pushEnabled = false;
            reason = "muted";
        } else if ("push_only".equals(preference)) {
            realtimeEnabled = false;
            pushEnabled = true;
            reason = "push_only";
        } else if ("realtime_only".equals(preference)) {
            realtimeEnabled = true;
            pushEnabled = false;
            reason = "realtime_only";
        } else if (highPriorityMeetingInvite) {
            pushEnabled = true;
            reason = "high_priority_meeting_invite";
        } else if (presenceState == ClientPresenceState.OFFLINE) {
            reason = "offline";
        } else {
            reason = "online";
        }

        return NotificationPolicyDecision.builder()
                .presenceState(presenceState)
                .realtimeEnabled(realtimeEnabled)
                .pushEnabled(pushEnabled)
                .reason(reason)
                .build();
    }

    private ClientPresenceState resolvePresence(Long userId) {
        if (userId == null) {
            return ClientPresenceState.OFFLINE;
        }

        return onlineService.getUserNodeId(userId) == null
                ? ClientPresenceState.OFFLINE
                : ClientPresenceState.ONLINE_FOREGROUND;
    }
}
