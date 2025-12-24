package com.github.im.server.config.security;

import com.github.im.server.config.mult.SchemaContext;
import com.github.im.server.model.Company;
import com.github.im.server.model.User;
import com.github.im.server.repository.UserRepository;
import com.github.im.server.service.CompanyService;
import com.github.im.server.service.impl.security.UserSecurityService;
import com.github.im.server.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public  class JwtToUserAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserSecurityService userSecurityService;
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // 从 JWT 提取角色信息
//        Collection<SimpleGrantedAuthority> authorities = extractAuthorities(jwt);

        User user = userSecurityService.jwt2User(jwt);
        return new UsernamePasswordAuthenticationToken(user,user.getPasswordHash(), user.getAuthorities());

    }

}