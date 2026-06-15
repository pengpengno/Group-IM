package com.github.im.server.service.notification.impl;

import com.github.im.server.service.notification.ClientEvent;
import com.github.im.server.service.notification.PushEndpoint;
import com.github.im.server.service.notification.PushEndpointRegistry;
import com.github.im.server.service.notification.PushNotificationGateway;
import com.github.im.server.service.notification.PushNotificationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class CompositePushNotificationGateway implements PushNotificationGateway {

    private final PushEndpointRegistry pushEndpointRegistry;
    private final List<PushNotificationProvider> pushNotificationProviders;

    @Override
    public void send(ClientEvent event) {
        if (event == null || event.getReceiverId() == null) {
            return;
        }

        List<PushEndpoint> endpoints = pushEndpointRegistry.findByUserId(event.getReceiverId())
                .stream()
                .filter(PushEndpoint::isEnabled)
                .toList();

        if (endpoints.isEmpty()) {
            log.info(
                    "No push endpoints registered receiverId={}, eventType={}, deepLink={}",
                    event.getReceiverId(),
                    event.getEventType(),
                    event.getDeepLink()
            );
            return;
        }

        for (PushEndpoint endpoint : endpoints) {
            PushNotificationProvider provider = pushNotificationProviders.stream()
                    .filter(item -> item.supports(endpoint.getProviderType()))
                    .findFirst()
                    .orElse(null);

            if (provider == null) {
                log.warn(
                        "No push provider found receiverId={}, endpointId={}, providerType={}",
                        event.getReceiverId(),
                        endpoint.getEndpointId(),
                        endpoint.getProviderType()
                );
                continue;
            }

            provider.send(endpoint, event);
        }
    }
}
