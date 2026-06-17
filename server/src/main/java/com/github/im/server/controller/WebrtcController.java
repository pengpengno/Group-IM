package com.github.im.server.controller;

import com.github.im.server.config.webrtc.WebrtcConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/webrtc")
@RequiredArgsConstructor
public class WebrtcController {

    private static final String FALLBACK_STUN_URL = "stun:stun.l.google.com:19302";

    private final WebrtcConfig webrtcConfig;

    @GetMapping("/ice-servers")
    public List<WebrtcConfig.IceServerConfig> getIceServers() {
        List<WebrtcConfig.IceServerConfig> servers = new ArrayList<>();

        addConfiguredIceServers(servers);
        addTurnServers(servers);

        if (servers.isEmpty()) {
            servers.add(new WebrtcConfig.IceServerConfig(FALLBACK_STUN_URL, null, null));
        }

        return servers;
    }

    private void addConfiguredIceServers(List<WebrtcConfig.IceServerConfig> servers) {
        if (webrtcConfig.getIceServers() == null) {
            return;
        }

        for (WebrtcConfig.IceServerConfig config : webrtcConfig.getIceServers()) {
            String url = trimToNull(config.getUrl());
            if (url == null) {
                continue;
            }

            servers.add(new WebrtcConfig.IceServerConfig(
                url,
                trimToNull(config.getUsername()),
                trimToNull(config.getCredential())
            ));
        }
    }

    private void addTurnServers(List<WebrtcConfig.IceServerConfig> servers) {
        if (!webrtcConfig.isTurnEnabled() || webrtcConfig.getTurnServer() == null) {
            return;
        }

        WebrtcConfig.TurnServerConfig turn = webrtcConfig.getTurnServer();
        String baseUrl = trimToNull(turn.getUrl());
        if (baseUrl == null) {
            return;
        }

        String username = trimToNull(turn.getUsername());
        String credential = trimToNull(turn.getCredential());
        String[] protocols = turn.getProtocols();

        Set<String> urls = new LinkedHashSet<>();
        if (protocols != null && protocols.length > 0) {
            for (String protocol : protocols) {
                String normalizedProtocol = trimToNull(protocol);
                if (normalizedProtocol == null) {
                    continue;
                }

                if (baseUrl.contains("?transport=")) {
                    urls.add(baseUrl);
                } else {
                    urls.add(baseUrl + "?transport=" + normalizedProtocol.toLowerCase());
                }
            }
        }

        if (urls.isEmpty()) {
            urls.add(baseUrl);
        }

        for (String url : urls) {
            servers.add(new WebrtcConfig.IceServerConfig(url, username, credential));
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
