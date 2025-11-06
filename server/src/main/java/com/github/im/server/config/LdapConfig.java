package com.github.im.server.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

/**
 * LDAP配置类
 * 仅在启用LDAP时加载
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.ldap", name = "urls")
public class LdapConfig {

    /**
     * LDAP上下文源配置
     * @return LdapContextSource
     */
    @Bean
    public LdapContextSource contextSource() {
        return new LdapContextSource();
    }

    /**
     * LDAP模板
     * @param contextSource LDAP上下文源
     * @return LdapTemplate
     */
    @Bean
    public LdapTemplate ldapTemplate(LdapContextSource contextSource) {
        return new LdapTemplate(contextSource);
    }
}