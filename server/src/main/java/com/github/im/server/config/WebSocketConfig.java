package com.github.im.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 配置消息代理
        config.enableSimpleBroker("/topic", "/queue");
        // 配置应用程序目标前缀
        config.setApplicationDestinationPrefixes("/app");
        // 配置用户目标前缀
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册STOMP端点
        registry.addEndpoint("/webrtc")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        
        // 注册备用端点（不使用SockJS）
        registry.addEndpoint("/webrtc")
                .setAllowedOriginPatterns("*");
    }
    
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        // 增加消息大小限制
        registry.setMessageSizeLimit(1024 * 1024); // 1MB
        registry.setSendBufferSizeLimit(1024 * 1024); // 1MB
        registry.setSendTimeLimit(20 * 10000); // 200 seconds
    }
}