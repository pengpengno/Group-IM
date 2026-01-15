package com.github.im.server.config.webrtc;

import com.github.im.server.handler.SignalWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 * 配置WebRTC信令WebSocket端点
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SignalWebSocketHandler signalWebSocketHandler;

    public WebSocketConfig(SignalWebSocketHandler signalWebSocketHandler) {
        this.signalWebSocketHandler = signalWebSocketHandler;
    }

    /**
     * 注册WebSocket处理器
     * 配置WebSocket端点和允许的跨域来源
     *
     * @param registry WebSocket处理器注册表
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

        registry.addHandler(signalWebSocketHandler, "/ws")
                .setAllowedOriginPatterns("*");
    }
}