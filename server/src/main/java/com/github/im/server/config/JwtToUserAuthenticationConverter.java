package com.github.im.server.config;

import com.github.im.server.model.User;
import com.github.im.server.repository.UserRepository;
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
    private final UserRepository userRepository;
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // 从 JWT 提取角色信息
        Collection<SimpleGrantedAuthority> authorities = extractAuthorities(jwt);


        long userId = Long.parseLong(jwt.getId());
        Optional<User> userOptional = userRepository.findById(userId);

        // 将 JWT 中的 claim 映射为你的 User 实体

        return userOptional.map(user-> {

            return new UsernamePasswordAuthenticationToken(user,user.getPasswordHash(), user.getAuthorities());
//            new UsernamePasswordAuthenticationToken(user, jwt.getTokenValue(), authorities)
        }).orElseThrow(()-> new BadCredentialsException("Invalid refresh token"));
    }

    @SuppressWarnings("unchecked")
    private Collection<SimpleGrantedAuthority> extractAuthorities(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) return List.of();
        return roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }
}