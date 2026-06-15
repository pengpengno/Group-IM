package com.github.im.server.service.notification;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NotificationPolicyDecision {
    ClientPresenceState presenceState;
    boolean realtimeEnabled;
    boolean pushEnabled;
    String reason;
}
