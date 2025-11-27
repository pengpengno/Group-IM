package com.github.im.server

import com.github.im.server.config.SecurityConfig
import com.github.im.server.service.impl.security.RefreshAuthenticationProvider
import com.github.im.server.service.impl.security.UserDetailsServiceImpl
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider
import spock.lang.Specification

class SecurityConfigSpec extends Specification {

    def "test authenticationManager includes LDAP provider when available"() {
        given: "SecurityConfig and mocked dependencies"
        SecurityConfig securityConfig = new SecurityConfig()
        HttpSecurity http = Mock()
        AuthenticationManagerBuilder authManagerBuilder = Mock()
        UserDetailsServiceImpl userService = Mock()
        RefreshAuthenticationProvider refreshAuthProvider = Mock()
        LdapAuthenticationProvider ldapAuthProvider = Mock()

        http.getSharedObject(AuthenticationManagerBuilder) >> authManagerBuilder
        authManagerBuilder.build() >> Mock(ProviderManager)

        when: "authentication manager is created with LDAP provider"
        def authManager = securityConfig.authenticationManager(http, userService, refreshAuthProvider, ldapAuthProvider)

        then: "authentication providers are registered correctly"
        authManager != null
        1 * authManagerBuilder.authenticationProvider(refreshAuthProvider)
        1 * authManagerBuilder.authenticationProvider(_)
        1 * authManagerBuilder.authenticationProvider(ldapAuthProvider)
        1 * authManagerBuilder.build()
    }

    def "test authenticationManager works without LDAP provider"() {
        given: "SecurityConfig and mocked dependencies without LDAP provider"
        SecurityConfig securityConfig = new SecurityConfig()
        HttpSecurity http = Mock()
        AuthenticationManagerBuilder authManagerBuilder = Mock()
        UserDetailsServiceImpl userService = Mock()
        RefreshAuthenticationProvider refreshAuthProvider = Mock()

        http.getSharedObject(AuthenticationManagerBuilder) >> authManagerBuilder
        authManagerBuilder.build() >> Mock(ProviderManager)

        when: "authentication manager is created without LDAP provider"
        def authManager = securityConfig.authenticationManager(http, userService, refreshAuthProvider, null)

        then: "authentication providers are registered correctly without LDAP"
        authManager != null
        1 * authManagerBuilder.authenticationProvider(refreshAuthProvider)
        1 * authManagerBuilder.authenticationProvider(_)
        0 * authManagerBuilder.authenticationProvider(_ instanceof LdapAuthenticationProvider)
        1 * authManagerBuilder.build()
    }
}