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
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshAuthenticationProvider implements AuthenticationProvider {

    private final UserSecurityService userSecurityService;
    private final JwtUtil jwtUtil;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String refreshToken = (String) authentication.getCredentials();

        var jwtOpt = jwtUtil.getJwt(refreshToken);
        if (jwtOpt.isEmpty()) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        var jwt = jwtOpt.get();
        User user = userSecurityService.jwt2User(jwt);
        return new UsernamePasswordAuthenticationToken(user,user.getPasswordHash(), user.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return RefreshAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
