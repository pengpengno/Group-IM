package com.github.im.server.service.impl.security;

import com.github.im.server.model.User;
import com.github.im.server.repository.UserRepository;
import com.github.im.server.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.security.Security;

@Component
@RequiredArgsConstructor
public class RefreshAuthenticationProvider implements AuthenticationProvider {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String refreshToken = (String) authentication.getCredentials();

        if (!jwtUtil.validateRefreshToken(refreshToken) ) {
            throw new BadCredentialsException("无效或过期的 refresh token");
        }
        // 这里你可以从数据库查询 refreshToken 是否有效
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

//        return new RefreshAuthenticationToken(user, user.getAuthorities());
        // 复用 即可
        return new UsernamePasswordAuthenticationToken(user,user.getPasswordHash(), user.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return RefreshAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
