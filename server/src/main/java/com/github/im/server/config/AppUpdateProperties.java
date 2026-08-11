package com.github.im.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Release metadata for the Android APK. Keep artifacts on HTTPS object storage/CDN. */
@Configuration
@ConfigurationProperties(prefix = "group.app-update.android")
@Data
public class AppUpdateProperties {
    private boolean enabled = false;
    private int versionCode = 0;
    private String versionName = "";
    private String apkUrl = "";
    private String sha256 = "";
    private long sizeBytes = 0;
    private String changelog = "";
    private boolean forceUpdate = false;
    private int minSupportedVersionCode = 0;
}
