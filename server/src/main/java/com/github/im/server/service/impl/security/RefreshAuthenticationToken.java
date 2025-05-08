package com.github.im.server.service.impl.security;

import com.github.im.server.model.User;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class RefreshAuthenticationToken extends AbstractAuthenticationToken {
    private final String refreshToken;
    private final User principal;

    public RefreshAuthenticationToken(String refreshToken) {
        super(null);
        this.refreshToken = refreshToken;
        this.principal = null;
        setAuthenticated(false);
    }

    public RefreshAuthenticationToken(User principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.refreshToken = null;
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return refreshToken;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
