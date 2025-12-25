package com.github.im.server.config.security;

import com.github.im.server.config.mult.SchemaContext;
import com.github.im.server.model.User;
import com.github.im.server.utils.UserTokenManager;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
public class JwtToUserAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserSecurityService userSecurityService;
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // 从 JWT 提取角色信息

        User user = userSecurityService.jwt2User(jwt);

        return new UsernamePasswordAuthenticationToken(user,user.getPasswordHash(), user.getAuthorities());

    }

}