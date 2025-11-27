package com.github.im.server.config;

import com.github.im.server.model.User;
import com.github.im.server.service.AuthenticationService;
import com.github.im.server.service.impl.security.LdapUserDetailsMapper;
import com.github.im.server.service.impl.security.RefreshAuthenticationProvider;
import com.github.im.server.service.impl.security.UserDetailsServiceImpl;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig  {

    @Value("${jwt.public.key}")
    RSAPublicKey key;

    @Value("${jwt.private.key}")
    RSAPrivateKey priv;




    @Autowired
    private JwtToUserAuthenticationConverter jwtToUserAuthenticationConverter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


        @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http,
                                                       @Autowired UserDetailsServiceImpl userService,
                                                       @Autowired RefreshAuthenticationProvider refreshAuthenticationProvider,
                                                       @Autowired(required = false) LdapAuthenticationProvider ldapAuthenticationProvider
                                                       ) throws Exception {
        AuthenticationManagerBuilder auth = http.getSharedObject(AuthenticationManagerBuilder.class);
        auth.authenticationProvider(refreshAuthenticationProvider);

        // 添加DAO认证提供者
        DaoAuthenticationProvider daoAuthProvider = new DaoAuthenticationProvider();
        daoAuthProvider.setUserDetailsService(userService);
        daoAuthProvider.setPasswordEncoder(passwordEncoder());
        auth.authenticationProvider(daoAuthProvider);
        
        // 添加LDAP认证提供者（如果有配置）
        if (ldapAuthenticationProvider != null) {
            auth.authenticationProvider(ldapAuthenticationProvider);
        }
        
        var authenticationManager = auth.build();
        return authenticationManager;
    }
    

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,AuthenticationService authenticationService) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/users/register",
                                "/api/users/import-template",
                                "/api/users/import",
                                "/api/users/login",
                                "/static/**",
                                "/socket.io/**",
                                "/ws/**"  , // 信令服务器
                                "/rtc"  , // 信令服务器
                                "/rtc/*"  , // 信令服务器
                                "/webrtc/**",   // WebRTC信令服务器
                                "/websocket/**",   // WebSocket端点
                                "/signaling/**"    // 专用信令端点
                        )
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
//                        oauth2.jwt(Customizer.withDefaults())) // 启用 JWT 支持
                        oauth2.jwt(jwt->jwt.jwtAuthenticationConverter(jwtToUserAuthenticationConverter))) // 启用 JWT 支持
                .httpBasic(Customizer.withDefaults())
                .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling((exceptions) -> exceptions
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
                );
        ;

        return http.build();
    }


    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withPublicKey(this.key).build();
    }


    @Bean
    JwtEncoder jwtEncoder() {
        JWK jwk = new RSAKey.Builder(this.key).privateKey(this.priv).build();
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }


}