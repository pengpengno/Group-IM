package com.github.im.server.service.notification;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class PushEndpoint {
    String endpointId;
    Long userId;
    PushPlatform platform;
    PushProviderType providerType;
    String deviceId;
    String token;
    String endpointUrl;
    String p256dh;
    String auth;
    String locale;
    String appVersion;
    boolean sandbox;
    boolean enabled;
    long createdAt;
    long updatedAt;
}
