package com.github.im.server.config.webrtc;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "webrtc")
public class WebrtcConfig {
    
    /**
     * 会话超时时间（毫秒）
     */
    private long sessionTimeout = 300000; // 5分钟
    
    /**
     * ICE服务器配置
     */
    private IceServerConfig[] iceServers = {
        new IceServerConfig("stun:stun.l.google.com:19302", null, null)
    };
    
    /**
     * 是否启用TURN服务器
     */
    private boolean turnEnabled = false;
    
    /**
     * TURN服务器配置
     */
    private TurnServerConfig turnServer = new TurnServerConfig();
    
    @Data
    public static class IceServerConfig {
        private String url;
        private String username;
        private String credential;
        
        public IceServerConfig() {}
        
        public IceServerConfig(String url, String username, String credential) {
            this.url = url;
            this.username = username;
            this.credential = credential;
        }
    }
    
    @Data
    public static class TurnServerConfig {
        private String url = "turn:localhost:3478";
        private String username = "username";
        private String credential = "credential";
        private String[] protocols = {"udp", "tcp"};
    }
}