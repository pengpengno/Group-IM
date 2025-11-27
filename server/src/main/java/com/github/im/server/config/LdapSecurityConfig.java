package com.github.im.server.config;

import com.github.im.server.service.impl.security.LdapUserDetailsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;

import java.util.Arrays;

/**
 * LDAP安全配置类
 * 仅在启用LDAP时加载
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.ldap", name = "urls")
public class LdapSecurityConfig {

    @Autowired
    private Environment env;

    @Bean
    ContextSource contextSource() {
        String ldapUrls = env.getProperty("spring.ldap.urls");
        String managerDn = env.getProperty("spring.ldap.username");
        String managerPassword = env.getProperty("spring.ldap.password");
        String baseDn = env.getProperty("spring.ldap.base", "");

        LdapContextSource ldapContextSource = new LdapContextSource();
        ldapContextSource.setUrl(ldapUrls);
        ldapContextSource.setUserDn(managerDn);
        ldapContextSource.setPassword(managerPassword);
        ldapContextSource.setBase(baseDn);
        ldapContextSource.afterPropertiesSet(); // 初始化
        
        return ldapContextSource;
    }


    @Bean
    public LdapAuthenticationProvider ldapAuthenticationProvider(
            DefaultSpringSecurityContextSource contextSource,
            LdapUserDetailsMapper ldapUserDetailsMapper) {

        BindAuthenticator authenticator = new BindAuthenticator(contextSource);

        // 配置用户DN模式或用户搜索
        String userDnPattern = env.getProperty("spring.security.ldap.user-dn-pattern");
        if (userDnPattern != null && !userDnPattern.isEmpty()) {
            String[] userDnPatterns = userDnPattern.split(",");
            authenticator.setUserDnPatterns(userDnPatterns);
        } else {
            String userSearchBase = env.getProperty("spring.security.ldap.user-search-base", "");
            String userSearchFilter = env.getProperty("spring.security.ldap.user-search-filter", "(uid={0})");
            FilterBasedLdapUserSearch userSearch = new FilterBasedLdapUserSearch(userSearchBase, userSearchFilter, contextSource);
            authenticator.setUserSearch(userSearch);
        }

        // 配置组搜索
        String groupSearchBase = env.getProperty("spring.security.ldap.group-search-base", "");
        DefaultLdapAuthoritiesPopulator authoritiesPopulator = new DefaultLdapAuthoritiesPopulator(contextSource, groupSearchBase);
        authoritiesPopulator.setIgnorePartialResultException(true);

        // 创建LDAP认证提供者
        LdapAuthenticationProvider provider = new LdapAuthenticationProvider(authenticator, authoritiesPopulator);
        provider.setUserDetailsContextMapper(ldapUserDetailsMapper);
        return provider;
    }

    @Bean
    public AuthenticationManager ldapAuthenticationManager(
            HttpSecurity httpSecurity,
            LdapAuthenticationProvider ldapAuthenticationProvider) throws Exception {
        AuthenticationManagerBuilder auth = httpSecurity.getSharedObject(AuthenticationManagerBuilder.class);
        auth.authenticationProvider(ldapAuthenticationProvider);
        return auth.build();
    }
}