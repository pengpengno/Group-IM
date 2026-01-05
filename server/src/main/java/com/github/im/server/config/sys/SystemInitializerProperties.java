package com.github.im.server.config.sys;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "group.system.initializer")
public class SystemInitializerProperties {
    
    /**
     * 是否启用系统初始化
     */
    private boolean enabled = true;
    
    /**
     * 默认公司配置
     */
    private DefaultCompany defaultCompany = new DefaultCompany();
    
    /**
     * 管理员用户配置
     */
    private AdminUser adminUser = new AdminUser();
    
    @Data
    public static class DefaultCompany {
        /**
         * 公司名称
         */
        private String name = "public";
        
        /**
         * 数据库schema名称
         */
        private String schemaName = "public";
        
        /**
         * 是否激活
         */
        private boolean active = true;
    }
    
    @Data
    public static class AdminUser {
        /**
         * 管理员用户名
         */
        private String username = "admin";
        
        /**
         * 管理员邮箱
         */
        private String email = "admin@example.com";
        
        /**
         * 管理员电话号码
         */
        private String phoneNumber = "1234567890";
        
        /**
         * 管理员密码
         */
        private String password = "12345";
    }
}