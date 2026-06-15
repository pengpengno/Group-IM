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
public class WebPushNotificationProvider implements PushNotificationProvider {

    private final PushPayloadMapper pushPayloadMapper;

    @Override
    public boolean supports(PushProviderType providerType) {
        return providerType == PushProviderType.WEB_PUSH;
    }

    @Override
    public void send(PushEndpoint endpoint, ClientEvent event) {
        log.info(
                "WEB_PUSH placeholder endpointId={}, receiverId={}, eventType={}, endpointUrl={}, deepLink={}, payload={}",
                endpoint.getEndpointId(),
                event.getReceiverId(),
                event.getEventType(),
                endpoint.getEndpointUrl(),
                event.getDeepLink(),
                pushPayloadMapper.toDataPayload(event)
        );
    }
}
