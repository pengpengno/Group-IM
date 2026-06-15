package com.github.im.server.service.notification;

public interface PushNotificationGateway {

    void send(ClientEvent event);
}
