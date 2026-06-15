package com.github.im.dto.notification;

import lombok.Data;

@Data
public class PushEndpointUpsertRequest {
    private String endpointId;
    private String platform;
    private String provider;
    private String deviceId;
    private String token;
    private String endpointUrl;
    private String p256dh;
    private String auth;
    private String locale;
    private String appVersion;
    private Boolean sandbox;
    private Boolean enabled;
}
