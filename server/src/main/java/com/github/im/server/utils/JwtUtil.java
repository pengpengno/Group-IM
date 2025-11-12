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
import java.util.Optional;

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
                .id(user.getUserId().toString())
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
                .id(user.getUserId().toString())
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
     * 有效则返回 userId
     * 无效 则返回 Optional.empty()
     */
    public Optional<Long> validateRefreshToken(String refreshToken) {
        try {
            var jwt = jwtDecoder.decode(refreshToken);
            Instant expiration = jwt.getExpiresAt();
            boolean notExpired = expiration != null && expiration.isAfter(Instant.now());
            if (notExpired) {
                return Optional.of(Long.parseLong(jwt.getId()));
            }else {
                //  TODO  提示凭据 过期
                return Optional.empty();
            }
        } catch (JwtException e) {
            log.error("Invalid refresh token: {}", e.getMessage());
            return Optional.empty();

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