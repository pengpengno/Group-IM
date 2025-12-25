package com.github.im.server.utils;

import com.github.im.server.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtEncoder encoder;

    private final JwtDecoder jwtDecoder;

    private static final long ACCESS_TOKEN_EXPIRY = 3600L; // 1小时过期
    private static final long REFRESH_TOKEN_EXPIRY = 2592000L; // 30天过期

    public static String USER_NAME_FIELD = "name";
    public static String COMPANY_ID_FIELD = "companyId";
    public static String COMPANY_SCHEMA_FIELD = "companySchema";

    /**
     * 创建 JWT
     * @param user
     * @return AccessToken
     */
    public String createAccessToken(User user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(user.getUserId().toString())
                .issuer(user.getUserId().toString())
                .issuedAt(now)
                // jwt 中不设置过期 ， 通过缓存是否存在判断过期
//                .expiresAt(now.plus(ACCESS_TOKEN_EXPIRY, ChronoUnit.SECONDS)) // 设置为1小时过期
                .subject(user.getUsername())
                .claim(USER_NAME_FIELD, user.getUsername())
                // 当前登录公司 Id
                .claim(COMPANY_ID_FIELD, user.getCurrentCompany().getCompanyId())
                .claim(COMPANY_SCHEMA_FIELD, user.getCurrentCompany().getSchemaName())
                .build();

        var encode = encoder.encode(JwtEncoderParameters.from(claims));
        return encode
                .getTokenValue();
    }


    /**
     * 创建 Refresh Token
     * @param user
     * @return refreshToken
     */
    public String createRefreshToken(User user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(user.getUserId().toString())
                .issuer(user.getUserId().toString())
                .issuedAt(now)
                // 设置为 30 天过期，每次登录都刷新 
                .expiresAt(now.plus(REFRESH_TOKEN_EXPIRY, ChronoUnit.SECONDS)) // 设置为30天过期
                .subject(user.getUsername())
                .claim(USER_NAME_FIELD, user.getUsername())
                // 当前登录公司 Id
                .claim(COMPANY_ID_FIELD, user.getCurrentCompany().getCompanyId())
                .claim(COMPANY_SCHEMA_FIELD, user.getCurrentCompany().getSchemaName())
                .build();

        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * 验证 RefreshToken
     * 获取 Jwt
     * @param token
     * @return Optional
     */
    public Optional<Jwt> getJwt(String token) throws BadCredentialsException{

        try{
            var jwt = jwtDecoder.decode(token);
            Instant expiration = jwt.getExpiresAt();
            if(expiration == null){
                // 没设置过期时间 那么直接返回即可
                return Optional.of(jwt);
            }
            boolean notExpired = expiration.isAfter(Instant.now());
            if (notExpired) {
                return Optional.of(jwt);
            }else {
                throw new BadCredentialsException("身份已过期");
            }
        }
        catch (Exception exception){ // 使用通用异常处理
            log.error("Invalid refresh token: {}", exception.getMessage());
            return Optional.empty();
        }
    }
}