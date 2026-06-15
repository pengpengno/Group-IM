package com.github.im.server.service.notification;

public interface PushNotificationProvider {

    boolean supports(PushProviderType providerType);

    void send(PushEndpoint endpoint, ClientEvent event);
}
