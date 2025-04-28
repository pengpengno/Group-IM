package com.github.im.server.config;

import lombok.RequiredArgsConstructor;
import org.apache.tomcat.util.buf.HexUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {


    private final WebRtcHandler webRtcHandler;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final WebSocketHandshakeInterceptor webSocketHandshakeInterceptor;


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
//        registry.
        // 注册 WebSocket 路径，"/signaling" 是 WebSocket 请求的 URI
        registry.addHandler(new SignalingWebSocketHandler(), "/signaling");
//        registry.addHandler(webRtcHandler, "/webrtc")
        registry.addHandler(chatWebSocketHandler, "/webrtc")
                .addInterceptors(webSocketHandshakeInterceptor)
                .setAllowedOrigins("*"); // 允许所有的源进行连接（根据需要调整）
    }
}
