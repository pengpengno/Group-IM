package com.github.im.server.config.security;

import com.github.im.server.config.mult.SchemaContext;
import com.github.im.server.config.mult.TenantContextFilter;
import com.github.im.server.security.CompanyAccessDeniedHandler;
import com.github.im.server.service.AuthenticationService;
import com.github.im.server.service.impl.security.RefreshAuthenticationProvider;
import com.github.im.server.service.impl.security.UserDetailsServiceImpl;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig  {

    @Autowired
    private JwtToUserAuthenticationConverter jwtToUserAuthenticationConverter;

    @Autowired
    private CompanyAccessDeniedHandler companyAccessDeniedHandler;

    @Autowired
    private AccessTokenRefreshFilter accessTokenRefreshFilter;

    @Autowired
    private TenantContextFilter tenantContextFilter;


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }



    @PostConstruct
    public void setSecurityContextHolderStrategy() {
        // 设置为可继承的ThreadLocal策略，使子线程可以访问父线程的安全上下文
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);

        System.setProperty("spring.security.strategy", SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
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
                                "/api/auth/logout",
                                "/static/**",
                                "/socket.io/**",
                                "/ws/**"  , // 信令服务器
                                "/signaling/**"    // 专用信令端点
                        )
                        .permitAll()
                        .anyRequest()
                        .authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt->jwt.jwtAuthenticationConverter(jwtToUserAuthenticationConverter)))
                .addFilterAfter(tenantContextFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(accessTokenRefreshFilter, BearerTokenAuthenticationFilter.class)
                .httpBasic(Customizer.withDefaults())
                .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling((exceptions) -> exceptions
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(companyAccessDeniedHandler)
                );
        ;

        return http.build();
    }

}