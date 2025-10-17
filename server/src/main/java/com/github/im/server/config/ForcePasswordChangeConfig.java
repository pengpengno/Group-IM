package com.github.im.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security")
public class ForcePasswordChangeConfig {
    
    /**
     * 是否启用强制密码修改功能
     */
    private boolean forcePasswordChangeEnabled = true;
    
    /**
     * 默认密码，新导入的用户将使用此密码
     */
    private String defaultPassword = "12345";
    
    public boolean isForcePasswordChangeEnabled() {
        return forcePasswordChangeEnabled;
    }
    
    public void setForcePasswordChangeEnabled(boolean forcePasswordChangeEnabled) {
        this.forcePasswordChangeEnabled = forcePasswordChangeEnabled;
    }
    
    public String getDefaultPassword() {
        return defaultPassword;
    }
    
    public void setDefaultPassword(String defaultPassword) {
        this.defaultPassword = defaultPassword;
    }
}