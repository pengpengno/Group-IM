package com.github.im.server.utils;

import cn.hutool.jwt.Claims;
import com.github.im.server.model.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtUtil {


    @Autowired
    public JwtEncoder encoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    private static final long ACCESS_TOKEN_EXPIRY = 3600L; // 1 hour
    private static final long REFRESH_TOKEN_EXPIRY = 2592000L; // 30 days


    public String createToken(User user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(user.getUserId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(ACCESS_TOKEN_EXPIRY, ChronoUnit.SECONDS)) // 设置为1小时过期
                .subject(user.getUsername())
                .claim("name", user.getUsername())
                .build();

        var encode = encoder.encode(JwtEncoderParameters.from(claims));
        var tokenValue = encode
                .getTokenValue();
        return tokenValue;
    }


    /**
     * 生成长期的 RefreshToken
     */
    public String createRefreshToken(User user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(user.getUserId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(REFRESH_TOKEN_EXPIRY, ChronoUnit.SECONDS)) // 设置为30天过期
                .subject(user.getUsername())
                .claim("name", user.getUsername())
                .build();

        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * 验证 RefreshToken 是否有效
     */
    public boolean validateRefreshToken(String refreshToken) {
        try {
            var jwt = jwtDecoder.decode(refreshToken);
            Instant expiration = jwt.getExpiresAt();
            return expiration != null && expiration.isAfter(Instant.now());
        } catch (JwtException e) {
            return false;
        }
    }

    public String  parseTokenAndGetName(String authToken){
        Assert.notNull(authToken,"Auth token not be null. ");
        var jwt = jwtDecoder.decode(authToken);

        var username = jwt.getClaimAsString("name");

        return username;
    }


    public Long parseToken(String authToken){
        Assert.notNull(authToken,"Auth token not be null. ");
        var jwt = jwtDecoder.decode(authToken);

        var userId = jwt.getClaimAsString(JwtClaimNames.ISS);

        return Long.parseLong(userId);
    }




}