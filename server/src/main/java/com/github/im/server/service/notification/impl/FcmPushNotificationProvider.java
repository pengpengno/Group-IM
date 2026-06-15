package com.github.im.server.service.notification.impl;

import com.github.im.server.service.notification.ClientEvent;
import com.github.im.server.service.notification.PushEndpoint;
import com.github.im.server.service.notification.PushNotificationProvider;
import com.github.im.server.service.notification.PushPayloadMapper;
import com.github.im.server.service.notification.PushProviderType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmPushNotificationProvider implements PushNotificationProvider {

    private final PushPayloadMapper pushPayloadMapper;

    @Override
    public boolean supports(PushProviderType providerType) {
        return providerType == PushProviderType.FCM;
    }

    @Override
    public void send(PushEndpoint endpoint, ClientEvent event) {
        log.info(
                "FCM placeholder push endpointId={}, receiverId={}, eventType={}, collapseKey={}, deepLink={}, payload={}",
                endpoint.getEndpointId(),
                event.getReceiverId(),
                event.getEventType(),
                event.getCollapseKey(),
                event.getDeepLink(),
                pushPayloadMapper.toDataPayload(event)
        );
    }
}
