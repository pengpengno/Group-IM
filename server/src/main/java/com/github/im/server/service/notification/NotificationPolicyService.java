package com.github.im.server.service.notification;

public interface NotificationPolicyService {

    NotificationPolicyDecision decide(ClientEvent event);
}
