package com.github.im.dto.notification;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PushEndpointDTO {
    String endpointId;
    String platform;
    String provider;
    String deviceId;
    String locale;
    String appVersion;
    boolean sandbox;
    boolean enabled;
    long createdAt;
    long updatedAt;
}
