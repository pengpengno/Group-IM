package com.github.im.server.utils;

import com.github.im.server.config.mult.SchemaContext;
import com.github.im.server.constants.CacheKeyConstants;
import com.github.im.server.model.Company;
import com.github.im.server.model.User;
import com.github.im.server.repository.UserRepository;
import com.github.im.server.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserTokenManager {

    private final RedisTemplate<String, Object> redisTemplate;

    private final JwtUtil jwtUtil; // 获取JWT工具类

    private static final long DEFAULT_EXPIRY_SECONDS = 3600L; // 默认1小时过期

    private final UserRepository userRepository;
    private final CompanyService companyService;

    /**
     * 创建用户访问令牌缓存
     * 1. 创建 AccessToken
     * 2. 缓存 过期时间为 1 小时
     * 3. 返回 AccessToken
     * @param user 用户
     */
    public String createAccessTokenAndCache(User user) {
        String accessToken = jwtUtil.createAccessToken(user);
        String cacheKey = buildCacheKey(user.getUserId());
        redisTemplate.opsForValue().set(cacheKey, accessToken, DEFAULT_EXPIRY_SECONDS, TimeUnit.SECONDS);
        log.debug("Created access token cache for user: {}, key: {}", user.getUserId(), cacheKey);
        return accessToken;
    }

    public User jwt2User(String token) throws BadCredentialsException {
        Optional<Jwt> jwt = jwtUtil.getJwt(token);
        if (jwt.isEmpty()) {
            throw new BadCredentialsException("Invalid JWT");
        }
        return jwt2User(jwt.get());
    }

    /**
     * 此方法 不验证 JWT 的有效性
     * 只 将 jwt 转化为 user
     * 将 jwt 中 payload 的数据 转化为 user
     * @param jwt
     * @return 处理后的用户给
     */
    public User jwt2User(Jwt jwt) throws BadCredentialsException {
        long userId = Long.parseLong(jwt.getId());
        Optional<User> userOptional = userRepository.findById(userId);

        // 将 JWT 中的 claim 映射为 User 实体
        return userOptional.map(user-> {
            // 从JWT中提取公司ID并设置到用户对象
            Long companyId = jwt.getClaim(JwtUtil.COMPANY_ID_FIELD);
            String schemaCode = jwt.getClaim(JwtUtil.COMPANY_SCHEMA_FIELD);
            if (companyId != null) {
                var companyOpt =  companyService.findById(companyId);
                var company = companyOpt.orElseThrow(()-> new BadCredentialsException("当前公司不存在！"));
                user.setCurrentCompany(company);
                List<Company> companies = user.getCompanies(); // 加载 Company 对象
            }
            return user;
        }).orElseThrow(()-> new BadCredentialsException("Invalid refresh token"));
    }

    /**
     * 创建用户访问令牌
     * 这里目前直接 返回 refreshToken 即可
     * @param user 用户
     * @return 长期访问令牌
     */
    public String createRefreshToken(User user) {
        return jwtUtil.createRefreshToken(user);
    }

    /**
     * 延长用户访问令牌缓存时间
     * @param userId 用户ID
     * @return true表示会话存在并已续期，false表示会话不存在
     */
    public boolean extendAccessToken(Long userId) {
        String cacheKey = buildCacheKey(userId);
        Boolean exists = redisTemplate.hasKey(cacheKey);
        if (exists) {
            // 延长缓存时间
            redisTemplate.expire(cacheKey, DEFAULT_EXPIRY_SECONDS, TimeUnit.SECONDS);
            log.debug("Extended access token cache for user: {}, key: {}", userId, cacheKey);
            return true;
        } else {
            log.debug("Access token cache not found for user: {}, key: {}", userId, cacheKey);
            return false;
        }
    }

    /**
     * 获取用户访问令牌
     * @param userId 用户ID
     * @return 会话令牌，如果不存在则返回null
     */
    public String getAccessToken(Long userId) {
        String cacheKey = buildCacheKey(userId);
        Object token = redisTemplate.opsForValue().get(cacheKey);
        return token != null ? token.toString() : null;
    }

    /**
     * 验证用户访问令牌是否有效
     * @param userId 用户ID
     * @return true表示会话有效，false表示会话无效或不存在
     */
    public boolean isAccessTokenValid(Long userId) {
        return getAccessToken(userId) != null;
    }

    /**
     * 删除用户访问令牌缓存
     * @param userId 用户ID
     */
    public void removeAccessToken(Long userId) {
        String cacheKey = buildCacheKey(userId);
        redisTemplate.delete(cacheKey);
        log.debug("Removed access token cache for user: {}, key: {}", userId, cacheKey);
    }

    /**
     * 构建缓存键
     * @param userId 用户ID
     * @return 缓存键
     */
    private String buildCacheKey(Long userId) {
        return String.format(CacheKeyConstants.User.USER_ACCESS_TOKEN_FORMAT, userId);
    }
}