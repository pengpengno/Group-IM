package com.github.im.server.controller;

import com.github.im.server.config.webrtc.WebrtcConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * WebRTC 配置控制器
 * 用于向客户端提供 ICE 服务器（STUN/TURN）配置
 */
@RestController
@RequestMapping("/api/webrtc")
@RequiredArgsConstructor
public class WebrtcController {

    private final WebrtcConfig webrtcConfig;

    /**
     * 获取 ICE 服务器配置列表
     * @return ICE 服务器配置列表
     */
    @GetMapping("/ice-servers")
    public List<WebrtcConfig.IceServerConfig> getIceServers() {
        List<WebrtcConfig.IceServerConfig> servers = new ArrayList<>();
        
        // 1. 添加基础 STUN 服务器
        if (webrtcConfig.getIceServers() != null) {
            for (WebrtcConfig.IceServerConfig config : webrtcConfig.getIceServers()) {
                servers.add(config);
            }
        }
        
        // 2. 如果启用了 TURN，添加 TURN 服务器配置
        if (webrtcConfig.isTurnEnabled() && webrtcConfig.getTurnServer() != null) {
            WebrtcConfig.TurnServerConfig turn = webrtcConfig.getTurnServer();
            // 将 TURN 配置也作为 IceServer 返回给客户端
            servers.add(new WebrtcConfig.IceServerConfig(
                turn.getUrl(),
                turn.getUsername(),
                turn.getCredential()
            ));
        }
        
        return servers;
    }
}
