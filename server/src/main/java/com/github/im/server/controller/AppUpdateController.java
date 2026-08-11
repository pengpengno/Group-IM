package com.github.im.server.controller;

import com.github.im.server.config.AppUpdateProperties;
import com.github.im.server.web.ApiResponse;
import com.github.im.server.web.ResponseUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.regex.Pattern;

/** Public bootstrap endpoint: it deliberately contains no tenant or account data. */
@RestController
@RequestMapping("/api/app-updates")
@RequiredArgsConstructor
public class AppUpdateController {
    private static final String ANDROID_PACKAGE = "com.github.im.group";
    private static final Pattern SHA_256 = Pattern.compile("^[a-f0-9]{64}$");
    private final AppUpdateProperties properties;

    @GetMapping("/android")
    public ResponseEntity<ApiResponse<AndroidUpdateView>> getAndroidUpdate(
        @RequestParam String packageName,
        @RequestParam int versionCode,
        @RequestParam(defaultValue = "stable") String channel
    ) {
        if (!ANDROID_PACKAGE.equals(packageName) || !"stable".equals(channel) || !isPublished()) {
            return ResponseUtil.success("No Android update available", null);
        }
        if (versionCode >= properties.getVersionCode()) {
            return ResponseUtil.success("Already on the latest version", null);
        }
        return ResponseUtil.success("Android update available", new AndroidUpdateView(properties));
    }

    private boolean isPublished() {
        return properties.isEnabled()
            && properties.getVersionCode() > 0
            && properties.getSizeBytes() > 0
            && properties.getApkUrl() != null && properties.getApkUrl().startsWith("https://")
            && properties.getSha256() != null
            && SHA_256.matcher(properties.getSha256().toLowerCase(Locale.ROOT)).matches();
    }

    @Getter
    public static class AndroidUpdateView {
        private final int versionCode;
        private final String versionName;
        private final String apkUrl;
        private final String sha256;
        private final long sizeBytes;
        private final String changelog;
        private final boolean forceUpdate;
        private final int minSupportedVersionCode;

        AndroidUpdateView(AppUpdateProperties source) {
            versionCode = source.getVersionCode(); versionName = source.getVersionName(); apkUrl = source.getApkUrl();
            sha256 = source.getSha256().toLowerCase(Locale.ROOT); sizeBytes = source.getSizeBytes();
            changelog = source.getChangelog(); forceUpdate = source.isForceUpdate();
            minSupportedVersionCode = source.getMinSupportedVersionCode();
        }
    }
}
