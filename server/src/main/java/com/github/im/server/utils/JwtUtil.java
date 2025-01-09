package com.github.im.server.utils;

import cn.hutool.jwt.Claims;
import com.github.im.server.model.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.net.URL;
import java.time.Instant;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {


    @Autowired
    JwtEncoder encoder;

    @Autowired
    JwtDecoder jwtDecoder;


    public String createToken(User user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(user.getUserId().toString())
                .issuedAt(now)
//                .expiresAt(now.plusSeconds(expiry))
                .subject(user.getUsername())
                .claim("name", user.getUsername())
                .build();

        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

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