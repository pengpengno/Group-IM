package com.github.im.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册 WebSocket 路径，"/signaling" 是 WebSocket 请求的 URI
        registry.addHandler(new SignalingWebSocketHandler(), "/webrtc")
                .setAllowedOrigins("*"); // 允许所有的源进行连接（根据需要调整）
    }
}
